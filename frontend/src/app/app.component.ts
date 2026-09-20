import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { UmlService, UmlClassDto } from './services/uml.service';
import { UmlWebSocketService, ClassCreatedEvent, WsStatus } from './services/uml-websocket.service';
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
  private eventBuffer: ClassCreatedEvent[] = [];
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

        // Paso 3: aplicar eventos bufferizados cuya versión sea mayor al snapshot
        // (pueden haber llegado por WS mientras el GET estaba en vuelo)
        for (const bufferedEvent of this.eventBuffer) {
          if (bufferedEvent.modelVersion > this.currentVersion) {
            this.applyEvent(bufferedEvent);
          }
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
          this.errorMessage = 'Conflicto de versión: recarga el modelo (F5)';
        } else if (err.status === 404) {
          this.errorMessage = 'Proyecto no encontrado';
        } else {
          this.errorMessage = 'Error: ' + (err.error?.error || err.message);
        }
      }
    });
  }

  /**
   * Aplica un evento CLASS_CREATED recibido por WebSocket.
   * Usa upsert por classId para evitar duplicados cuando el mismo
   * navegador recibe: POST response + su propio broadcast CLASS_CREATED.
   */
  private applyEvent(event: ClassCreatedEvent): void {
    this.upsertClass({ id: event.classId, name: event.className });
    // version = max(local, recibida)
    this.currentVersion = Math.max(this.currentVersion, event.modelVersion);
    this.eventLog.unshift(`CLASS_CREATED "${event.className}" v${event.modelVersion}`);
    // Mantener log acotado
    if (this.eventLog.length > 20) {
      this.eventLog.pop();
    }
  }

  private upsertClass(cls: UmlClassDto): void {
    const existing = this.classes.findIndex(c => c.id === cls.id);
    if (existing === -1) {
      this.classes = [...this.classes, cls];
    }
    // Si ya existe, no duplicar
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.wsService.disconnect();
  }
}
