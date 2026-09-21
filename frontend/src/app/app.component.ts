import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { UmlService, UmlClassDto } from './services/uml.service';
import { UmlWebSocketService, ClassCreatedEvent, ClassRenamedEvent, UmlEvent, WsStatus } from './services/uml-websocket.service';
import { v4 as uuidv4 } from 'uuid';

// UUID del proyecto temporal de desarrollo (Fase 1)
const PROJECT_ID = '00000000-0000-0000-0000-000000000001';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {

  projectId = PROJECT_ID;
  currentVersion = 0;
  wsStatus: WsStatus = 'disconnected';
  classes: UmlClassDto[] = [];
  eventLog: string[] = [];
  newClassName = '';
  errorMessage = '';

  // Buffer de eventos que llegan antes de que el GET snapshot termine
  private eventBuffer: UmlEvent[] = [];
  private snapshotLoaded = false;
  private wsSub?: Subscription;

  constructor(
    private umlService: UmlService,
    private wsService: UmlWebSocketService
  ) {}

  async ngOnInit(): Promise<void> {
    // ORDEN CRÍTICO:
    // 1. Conectar WebSocket y suscribirse al topic PRIMERO
    // 2. Después hacer GET del snapshot inicial
    // Esto evita perder eventos que lleguen entre GET y subscribe

    this.wsService.status$.subscribe(status => {
      this.wsStatus = status;
    });

    // Bufferizar eventos que lleguen antes de que el GET termine
    this.wsSub = this.wsService.events$.subscribe(event => {
      if (!this.snapshotLoaded) {
        this.eventBuffer.push(event);
      } else {
        this.applyEvent(event);
      }
    });

    // Paso 1: conectar y suscribirse al topic
    await this.wsService.connect(this.projectId);

    // Paso 2: obtener snapshot inicial
    this.loadModel();
  }

  private loadModel(): void {
    this.umlService.getModel(this.projectId).subscribe({
      next: (model) => {
        // Renderizar snapshot
        this.currentVersion = model.version;
        this.classes = [...model.classes];
        this.snapshotLoaded = true;

        // Paso 3: aplicar eventos bufferizados usando la misma regla estricta de secuencia
        // (pueden haber llegado por WS mientras el GET estaba en vuelo)
        for (const bufferedEvent of this.eventBuffer) {
          this.applyEvent(bufferedEvent);
        }
        this.eventBuffer = [];
      },
      error: (err) => {
        this.errorMessage = 'Error cargando el modelo: ' + err.message;
        this.snapshotLoaded = true;
        this.eventBuffer = [];
      }
    });
  }

  createClass(): void {
    const name = this.newClassName.trim();
    if (!name) return;

    this.errorMessage = '';

    this.umlService.createClass(this.projectId, {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion,
      name
    }).subscribe({
      next: (response) => {
        // A actualiza su versión con la versión real persistida
        // (sin depender de recibir su propio broadcast)
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        // Upsert: añadir si no existe (el broadcast propio puede llegar también)
        this.upsertClass({ id: response.classId, name: response.className });
        this.newClassName = '';
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
          this.loadModel();
        } else if (err.status === 404) {
          this.errorMessage = 'Proyecto no encontrado';
        } else {
          this.errorMessage = 'Error: ' + (err.error?.error || err.message);
        }
      }
    });
  }

  /**
   * Aplica un evento recibido por WebSocket según protocolo estricto de secuencia:
   *
   *   event.modelVersion <= currentVersion    → ignorar (duplicado / desordenado)
   *   event.modelVersion == currentVersion + 1 → aplicar incrementalmente
   *   event.modelVersion >  currentVersion + 1 → gap detectado → GET /model
   *
   * Esta regla garantiza consistencia causal: nunca aplicamos eventos fuera de orden
   * y detectamos huecos en la secuencia para reconstruir el snapshot completo.
   */
  private applyEvent(event: UmlEvent): void {
    if (event.modelVersion <= this.currentVersion) {
      // Duplicado o evento ya procesado — ignorar silenciosamente
      return;
    }

    if (event.modelVersion > this.currentVersion + 1) {
      // Gap detectado: versiones intermedias perdidas → resincronización completa
      this.loadModel();
      return;
    }

    // event.modelVersion === currentVersion + 1 → aplicar incrementalmente
    if ('newName' in event) {
      // Es ClassRenamedEvent
      this.upsertClass({ id: event.classId, name: event.newName });
      this.currentVersion = event.modelVersion;
      this.eventLog.unshift(`CLASS_RENAMED "${event.newName}" v${event.modelVersion}`);
    } else {
      // Es ClassCreatedEvent
      this.upsertClass({ id: event.classId, name: event.className });
      this.currentVersion = event.modelVersion;
      this.eventLog.unshift(`CLASS_CREATED "${event.className}" v${event.modelVersion}`);
    }

    // Mantener log acotado
    if (this.eventLog.length > 20) {
      this.eventLog.pop();
    }
  }

  renameClass(cls: UmlClassDto): void {
    const newName = prompt('Nuevo nombre:', cls.name);
    if (!newName || newName.trim() === '' || newName.trim() === cls.name) return;

    this.errorMessage = '';

    this.umlService.renameClass(this.projectId, cls.id, {
      commandId: uuidv4(),
      participantId: 'browser-A', // o browser-B para testing
      expectedVersion: this.currentVersion,
      newName: newName.trim()
    }).subscribe({
      next: (response) => {
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        this.upsertClass({ id: response.classId, name: response.newName });
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
          this.loadModel();
        } else if (err.status === 404) {
          this.errorMessage = 'Proyecto o Clase no encontrado';
        } else {
          this.errorMessage = 'Error: ' + (err.error?.error || err.message);
        }
      }
    });
  }

  private upsertClass(cls: UmlClassDto): void {
    const existing = this.classes.findIndex(c => c.id === cls.id);
    if (existing === -1) {
      this.classes = [...this.classes, cls];
    } else {
      // Actualizar si existe (para rename)
      const updatedClasses = [...this.classes];
      updatedClasses[existing] = cls;
      this.classes = updatedClasses;
    }
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.wsService.disconnect();
  }
}
