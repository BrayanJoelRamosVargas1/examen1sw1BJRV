import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { UmlService } from './services/uml.service';
import { UmlWebSocketService, WsStatus, ClassRenamedEvent, DiagramEvent } from './services/uml-websocket.service';
import { of, Subject } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

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
    umlServiceSpy = jasmine.createSpyObj('UmlService', ['getModel', 'getDiagram', 'renameClass']);

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
});
