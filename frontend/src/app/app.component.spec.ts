import { TestBed, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { UmlService } from './services/uml.service';
import { UmlWebSocketService, WsStatus, ClassRenamedEvent, DiagramEvent } from './services/uml-websocket.service';
import { of, Subject, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { VoiceCommandType } from './services/voice/voice-command-parser';

/**
 * Crea un evento CLASS_RENAMED simulado con los campos mínimos necesarios.
 */
function makeRenamedEvent(classId: string, newName: string, modelVersion: number): ClassRenamedEvent {
  return {
    eventType: 'CLASS_RENAMED',
    projectId: 'test-project',
    commandId: `cmd-${modelVersion}`,
    classId,
    newName,
    modelVersion
  } as unknown as ClassRenamedEvent;
}

/**
 * Snapshot de modelo mínimo para devolver en un mock de getModel().
 */
function makeModelSnapshot(version: number, id = 'c1', name = 'C1') {
  return of({
    version,
    classes: [{ id, name, attributes: [], operations: [] }],
    relationships: []
  } as any);
}

describe('AppComponent (Robustez STOMP)', () => {
  let umlServiceSpy: jasmine.SpyObj<UmlService>;
  let wsServiceMock: Partial<UmlWebSocketService>;
  let eventsSubject: Subject<any>;
  let layoutEventsSubject: Subject<DiagramEvent>;
  let statusSubject: Subject<WsStatus>;

  beforeEach(async () => {
    umlServiceSpy = jasmine.createSpyObj('UmlService', [
      'getModel', 'getDiagram', 'renameClass', 'interpretNaturalLanguage',
      'interpretUmlImage', 'askUmlAssistant', 'createClass', 'addAttribute', 'addOperation', 'addRelationship'
    ]);

    eventsSubject = new Subject();
    layoutEventsSubject = new Subject();
    statusSubject = new Subject();

    wsServiceMock = {
      connect: jasmine.createSpy('connect').and.returnValue(Promise.resolve()),
      disconnect: jasmine.createSpy('disconnect'),
      events$: eventsSubject.asObservable(),
      layoutEvents$: layoutEventsSubject.asObservable(),
      status$: statusSubject.asObservable()
    };

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UmlService, useValue: umlServiceSpy },
        { provide: UmlWebSocketService, useValue: wsServiceMock }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  });

  // ─── Test 1: Duplicado ─────────────────────────────────────────────────────
  it('should ignore duplicate events (same modelVersion)', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    umlServiceSpy.getModel.and.returnValue(makeModelSnapshot(20, 'c1', 'Clase'));
    umlServiceSpy.getDiagram.and.returnValue(of({ projectId: 'test-project', layoutVersion: 20, nodeViews: [] }));

    fixture.detectChanges();
    tick();

    expect(app.currentVersion).toBe(20);

    eventsSubject.next(makeRenamedEvent('c1', 'NuevaClase', 21));
    expect(app.currentVersion).toBe(21);
    expect(app.classes[0].name).toBe('NuevaClase');
    expect(app.eventLog.length).toBe(1);

    // Mismo v21: duplicado exacto (misma versión)
    eventsSubject.next(makeRenamedEvent('c1', 'NuevaClase2', 21));

    expect(app.currentVersion).toBe(21);
    expect(app.classes[0].name).toBe('NuevaClase'); // no se aplicó el segundo
    expect(app.eventLog.length).toBe(1);
  }));

  // ─── Test 2: Stale event ───────────────────────────────────────────────────
  it('should ignore stale events (delayed in network, modelVersion < currentVersion)', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    umlServiceSpy.getModel.and.returnValue(makeModelSnapshot(25, 'c1', 'C1'));
    umlServiceSpy.getDiagram.and.returnValue(of({ projectId: 'test-project', layoutVersion: 25, nodeViews: [] }));

    fixture.detectChanges();
    tick();

    expect(app.currentVersion).toBe(25);

    // Evento v24 llega tarde: debe ignorarse completamente
    eventsSubject.next(makeRenamedEvent('c1', 'C1_old', 24));

    expect(app.currentVersion).toBe(25);
    expect(app.classes[0].name).toBe('C1');
  }));

  // ─── Test 3: Gap ──────────────────────────────────────────────────────────
  it('should detect gap (modelVersion > currentVersion + 1) and trigger loadModel', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    umlServiceSpy.getModel.and.returnValue(makeModelSnapshot(30, 'c1', 'C1'));
    umlServiceSpy.getDiagram.and.returnValue(of({ projectId: 'test-project', layoutVersion: 30, nodeViews: [] }));

    fixture.detectChanges();
    tick();

    expect(app.currentVersion).toBe(30);
    umlServiceSpy.getModel.calls.reset();

    // v32 llega cuando currentVersion=30: gap de 1 versión perdida
    eventsSubject.next(makeRenamedEvent('c1', 'C1_v32', 32));

    expect(app.currentVersion).toBe(30);          // NO se aplica
    expect(umlServiceSpy.getModel).toHaveBeenCalledTimes(1); // loadModel() disparado
  }));

  // ─── Test 4: Stale GET no debe pisar currentVersion más nuevo ─────────────
  it('should ignore stale GET snapshot (model.version < currentVersion)', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    const getModelSubject = new Subject<any>();
    umlServiceSpy.getModel.and.returnValue(getModelSubject.asObservable());
    umlServiceSpy.getDiagram.and.returnValue(of({ projectId: 'test-project', layoutVersion: 41, nodeViews: [] }));

    // Estado inicial ya avanzado
    app.currentVersion = 41;
    app.classes = [{ id: 'c1', name: 'C1_v41', attributes: [], operations: [] }];

    // Dispara loadModel() — snapshotLoaded = false
    app['loadModel']();

    // GET retorna snapshot anticuado v40
    getModelSubject.next({
      version: 40,
      classes: [{ id: 'c1', name: 'C1_old', attributes: [], operations: [] }],
      relationships: []
    });

    // El estado v41 debe permanecer intacto
    expect(app.currentVersion).toBe(41);
    expect(app.classes[0].name).toBe('C1_v41');
  }));

  // ─── Test 5: Stale GET + evento bufferizado válido NO se descarta ──────────
  it('should NOT discard buffered events when stale GET arrives', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    // Primera carga: levantar ngOnInit para que la suscripción a events$ exista
    const firstLoadSubject = new Subject<any>();
    umlServiceSpy.getModel.and.returnValue(firstLoadSubject.asObservable());
    umlServiceSpy.getDiagram.and.returnValue(of({ projectId: 'test-project', layoutVersion: 41, nodeViews: [] }));

    fixture.detectChanges();
    tick();

    // Completar la primera carga con v41
    firstLoadSubject.next({
      version: 41,
      classes: [{ id: 'c1', name: 'C1_v41', attributes: [], operations: [] }],
      relationships: []
    });

    expect(app.currentVersion).toBe(41);
    expect(app['snapshotLoaded']).toBeTrue();

    // Segunda carga (ej.: gap detectado): loadModel() con GET que responderá tarde
    const stalGetSubject = new Subject<any>();
    umlServiceSpy.getModel.and.returnValue(stalGetSubject.asObservable());
    app['loadModel'](); // snapshotLoaded = false → buffer activo

    // Mientras GET está en vuelo, llega STOMP v42 → va al buffer
    eventsSubject.next(makeRenamedEvent('c1', 'C1_v42', 42));
    expect(app.currentVersion).toBe(41); // no aplicado aún

    // GET responde con v40 (stale: 40 < 41)
    // Guardia: NO sobreescribe, SÍ drena buffer → applyEvent(v42)
    stalGetSubject.next({
      version: 40,
      classes: [{ id: 'c1', name: 'C1_fromStaleGET', attributes: [], operations: [] }],
      relationships: []
    });

    // v42 debe haberse aplicado desde el buffer
    expect(app.currentVersion).toBe(42);
    expect(app.classes[0].name).toBe('C1_v42');
  }));

  // ─── Test 6: HTTP response vieja (renameClass) no retrocede currentVersion ─
  it('should ignore stale HTTP response from renameClass when STOMP already advanced', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    // Setup inicial: cargamos el modelo en v50
    umlServiceSpy.getModel.and.returnValue(makeModelSnapshot(50, 'c1', 'C1'));
    umlServiceSpy.getDiagram.and.returnValue(of({ projectId: 'test-project', layoutVersion: 50, nodeViews: [] }));

    fixture.detectChanges();
    tick();
    expect(app.currentVersion).toBe(50);

    // Controlamos la respuesta HTTP de renameClass con un Subject (en vuelo)
    const renameHttpSubject = new Subject<any>();
    umlServiceSpy.renameClass.and.returnValue(renameHttpSubject.asObservable());

    // El usuario lanza renameClass (HTTP inicia pero aún no responde)
    app['umlService'].renameClass(app['projectId'], 'c1', {
      commandId: 'cmd-test',
      participantId: 'browser-A',
      expectedVersion: 50,
      newName: 'NuevoNombre'
    }).subscribe({
      next: (response: any) => {
        // Misma lógica que el componente real: ignorar si modelVersion < currentVersion
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > app.currentVersion + 1) {
          app['loadModel']();
          return;
        }
        if (response.modelVersion < app.currentVersion) {
          return; // Stale HTTP — ignorar
        }
        app.currentVersion = response.modelVersion;
        const existing = app.classes.find((c: any) => c.id === response.classId);
        app['upsertClass']({ id: response.classId, name: response.newName, attributes: existing?.attributes ?? [], operations: existing?.operations ?? [] });
      }
    });

    // Antes de que responda HTTP, llega STOMP v51
    eventsSubject.next(makeRenamedEvent('c1', 'C1_viaSTOMP', 51));
    expect(app.currentVersion).toBe(51);
    expect(app.classes[0].name).toBe('C1_viaSTOMP');

    // Ahora responde HTTP con modelVersion=50 (respuesta antigua)
    renameHttpSubject.next({
      modelVersion: 50,
      classId: 'c1',
      className: 'NuevoNombre',
      newName: 'NuevoNombre'
    });

    // La respuesta HTTP vieja (50 < 51) debe ignorarse
    expect(app.currentVersion).toBe(51);
    expect(app.classes[0].name).toBe('C1_viaSTOMP');
  }));

  it('uses the deterministic parser without calling AI', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    app.processVoiceTranscript('crear clase Cliente');

    expect(app.voiceCommandPreview?.type).toBe(VoiceCommandType.CREATE_CLASS);
    expect(umlServiceSpy.interpretNaturalLanguage).not.toHaveBeenCalled();
  });

  it('calls AI only for an unknown transcript and shows a single-command preview', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    umlServiceSpy.interpretNaturalLanguage.and.returnValue(of([
      { type: 'CREATE_CLASS', className: 'Cliente' }
    ]));

    app.processVoiceTranscript('necesito una clase para clientes');

    expect(umlServiceSpy.interpretNaturalLanguage).toHaveBeenCalledWith(
      app.projectId, 'necesito una clase para clientes'
    );
    expect(app.aiCommandsPreview).toEqual([{ type: 'CREATE_CLASS', className: 'Cliente' }]);
  });

  it('shows a batch preview and cancellation performs no mutations', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    umlServiceSpy.interpretNaturalLanguage.and.returnValue(of([
      { type: 'CREATE_CLASS', className: 'Cliente' },
      { type: 'ADD_ATTRIBUTE', className: 'Cliente', attributeName: 'nombre', attributeType: 'String' }
    ]));

    app.processVoiceTranscript('crea Cliente con nombre');
    expect(app.aiCommandsPreview.length).toBe(2);

    app.cancelVoiceCommand();

    expect(app.aiCommandsPreview).toEqual([]);
    expect(umlServiceSpy.createClass).not.toHaveBeenCalled();
    expect(umlServiceSpy.addAttribute).not.toHaveBeenCalled();
  });

  it('executes an AI batch sequentially with the returned model version', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.currentVersion = 3;
    app.aiCommandsPreview = [
      { type: 'CREATE_CLASS', className: 'Cliente' },
      { type: 'ADD_ATTRIBUTE', className: 'Cliente', attributeName: 'nombre', attributeType: 'String' }
    ];
    umlServiceSpy.createClass.and.returnValue(of({ classId: 'c2', className: 'Cliente', modelVersion: 4 } as any));
    umlServiceSpy.addAttribute.and.returnValue(of({
      attribute: { id: 'a1', name: 'nombre', type: 'String' }, modelVersion: 5
    } as any));

    app.executeAiCommandsBatch();
    tick(50);
    tick(50);
    flushMicrotasks();

    expect(umlServiceSpy.createClass.calls.mostRecent().args[1].expectedVersion).toBe(3);
    expect(umlServiceSpy.addAttribute.calls.mostRecent().args[2].expectedVersion).toBe(4);
    expect(app.currentVersion).toBe(5);
  }));

  it('stops an AI batch on 409 and resynchronizes the model', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.aiCommandsPreview = [
      { type: 'CREATE_CLASS', className: 'Cliente' },
      { type: 'CREATE_CLASS', className: 'Pedido' }
    ];
    umlServiceSpy.createClass.and.returnValue(throwError(() => ({ status: 409 })));
    umlServiceSpy.getModel.and.returnValue(makeModelSnapshot(8));

    app.executeAiCommandsBatch();
    flushMicrotasks();

    expect(umlServiceSpy.createClass).toHaveBeenCalledTimes(1);
    expect(umlServiceSpy.getModel).toHaveBeenCalled();
    expect(app.voiceError).toContain('Conflicto');
  }));

  it('reports an unavailable AI provider without breaking deterministic parsing', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    umlServiceSpy.interpretNaturalLanguage.and.returnValue(
      throwError(() => ({ error: { error: 'Proveedor no disponible' } }))
    );

    app.processVoiceTranscript('necesito una clase');
    expect(app.voiceError).toContain('Proveedor no disponible');

    app.processVoiceTranscript('crear clase Producto');
    expect(app.voiceCommandPreview?.type).toBe(VoiceCommandType.CREATE_CLASS);
  });

  it('rejects an invalid image before making an HTTP request', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    const file = new File(['not an image'], 'diagram.svg', { type: 'image/svg+xml' });

    app.onUmlImageSelected({ target: { files: [file] } } as any);

    expect(app.imageError).toContain('Formato no permitido');
    expect(umlServiceSpy.interpretUmlImage).not.toHaveBeenCalled();
  });

  it('uploads a valid image and shows command preview plus warnings', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    umlServiceSpy.interpretUmlImage.and.returnValue(of({
      commands: [{ type: 'CREATE_CLASS', className: 'Cliente' }],
      warnings: ['Multiplicidad no legible']
    }));
    const file = new File(['png'], 'diagram.png', { type: 'image/png' });

    app.onUmlImageSelected({ target: { files: [file] } } as any);
    tick();

    expect(umlServiceSpy.interpretUmlImage).toHaveBeenCalledWith(app.projectId, file);
    expect(app.aiCommandsPreview[0].className).toBe('Cliente');
    expect(app.imageWarnings).toEqual(['Multiplicidad no legible']);
    expect(app.isProcessingImage).toBeFalse();
  }));

  it('cancelling image preview clears commands without mutating the model', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.imagePreview = 'data:image/png;base64,fixture';
    app.aiCommandsPreview = [{ type: 'CREATE_CLASS', className: 'Cliente' }];

    app.cancelVoiceCommand();

    expect(app.imagePreview).toBe('');
    expect(app.aiCommandsPreview).toEqual([]);
    expect(umlServiceSpy.createClass).not.toHaveBeenCalled();
  });

  it('displays assistant answer, findings and suggested commands', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    umlServiceSpy.askUmlAssistant.and.returnValue(of({
      answer: 'Tu modelo tiene una clase aislada.',
      findings: [{ code: 'ISOLATED_CLASS', severity: 'INFO', message: 'Cliente está aislada.' }],
      suggestedCommands: [{ type: 'CREATE_CLASS', className: 'Factura' }],
      warnings: []
    } as any));

    app.assistantMessage = '¿Qué le falta a mi modelo?';
    app.askAssistant();

    expect(umlServiceSpy.askUmlAssistant).toHaveBeenCalledWith(app.projectId, '¿Qué le falta a mi modelo?');
    expect(app.assistantAnswer).toContain('aislada');
    expect(app.assistantFindings.length).toBe(1);
    expect(app.assistantCommandsPreview[0].className).toBe('Factura');
  });

  it('discarding assistant suggestions performs zero mutations', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.assistantCommandsPreview = [{ type: 'CREATE_CLASS', className: 'Factura' }];

    app.discardAssistantSuggestions();

    expect(app.assistantCommandsPreview).toEqual([]);
    expect(umlServiceSpy.createClass).not.toHaveBeenCalled();
  });

  it('applies assistant suggestions through the existing batch executor', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.currentVersion = 2;
    app.assistantCommandsPreview = [{ type: 'CREATE_CLASS', className: 'Factura' }];
    umlServiceSpy.createClass.and.returnValue(of({ classId: 'c9', className: 'Factura', modelVersion: 3 } as any));

    app.applyAssistantSuggestions();
    tick(50);

    expect(umlServiceSpy.createClass).toHaveBeenCalled();
    expect(umlServiceSpy.createClass.calls.mostRecent().args[1].expectedVersion).toBe(2);
  }));

  it('shows a controlled error when the assistant backend fails', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    umlServiceSpy.askUmlAssistant.and.returnValue(throwError(() => ({ error: { error: 'Asistente no disponible' } })));

    app.assistantMessage = 'resume el modelo';
    app.askAssistant();

    expect(app.assistantError).toContain('Asistente no disponible');
  });

  it('renders the integral CASE entry points together', fakeAsync(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    app.wsStatus = 'connected';
    umlServiceSpy.getModel.and.returnValue(makeModelSnapshot(1));
    umlServiceSpy.getDiagram.and.returnValue(of({ projectId: app.projectId, layoutVersion: 1, nodeViews: [] }));

    fixture.detectChanges();
    tick();
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Modelar por voz');
    expect(text).toContain('Importar foto UML');
    expect(text).toContain('Asistente UML');
    expect(text).toContain('Exportar XMI 2.1');
    expect(text).toContain('Exportar SQL');
    expect(text).toContain('Generar Backend');
  }));
});
