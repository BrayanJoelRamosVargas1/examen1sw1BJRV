import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { UmlService, UmlClassDto } from './services/uml.service';
import { UmlWebSocketService, ClassCreatedEvent, ClassRenamedEvent, UmlEvent, WsStatus, AttributeUpdatedEvent } from './services/uml-websocket.service';
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

    // event.modelVersion === currentVersion + 1 -> aplicar incrementalmente
    if ('newName' in event) {
      // Es ClassRenamedEvent
      const targetClass = this.classes.find(c => c.id === event.classId);
      this.upsertClass({ id: event.classId, name: event.newName, attributes: targetClass ? targetClass.attributes : [] });
      this.currentVersion = event.modelVersion;
      this.eventLog.unshift(`CLASS_RENAMED "${event.newName}" v${event.modelVersion}`);
    } else if (('eventType' in event && event.eventType === 'ATTRIBUTE_UPDATED') || ('eventType' in event === false && 'attributeId' in event && !this.isNewAttribute(event.classId, (event as any).attributeId))) {
      // Es AttributeUpdatedEvent
      const targetClass = this.classes.find(c => c.id === event.classId);
      if (targetClass) {
        targetClass.attributes = targetClass.attributes || [];
        const attrIndex = targetClass.attributes.findIndex(a => a.id === event.attributeId);
        
        if (attrIndex !== -1) {
          // Update
          targetClass.attributes[attrIndex].name = event.name;
          targetClass.attributes[attrIndex].type = event.type;
          targetClass.attributes[attrIndex].visibility = event.visibility;
          this.eventLog.unshift(`ATTRIBUTE_UPDATED "${event.name}" v${event.modelVersion}`);
          this.upsertClass(targetClass);
          this.currentVersion = event.modelVersion;
        } else {
          // Update failed locally -> Snapshot
          this.loadModel();
          return;
        }
      } else {
        // En un caso real pediríamos snapshot al faltar la clase
        this.loadModel();
        return;
      }
    } else if (('eventType' in event && event.eventType === 'ATTRIBUTE_ADDED') || ('attributeId' in event)) {
      // Es AttributeAddedEvent
      const targetClass = this.classes.find(c => c.id === event.classId);
      if (targetClass) {
        targetClass.attributes = targetClass.attributes || [];
        targetClass.attributes.push({
          id: event.attributeId,
          name: event.name,
          type: event.type,
          visibility: event.visibility,
          orderIndex: event.orderIndex
        });
        this.eventLog.unshift(`ATTRIBUTE_ADDED "${event.name}" v${event.modelVersion}`);
        this.upsertClass(targetClass);
        this.currentVersion = event.modelVersion;
      } else {
        this.loadModel();
        return;
      }
    } else {
      // Es ClassCreatedEvent
      this.upsertClass({ id: event.classId, name: event.className, attributes: [] });
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
        const existing = this.classes.find(c => c.id === response.classId);
        this.upsertClass({ id: response.classId, name: response.newName, attributes: existing ? existing.attributes : [] });
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

  addAttribute(cls: UmlClassDto): void {
    const attrName = prompt('Nombre del nuevo atributo:');
    if (!attrName || attrName.trim() === '') return;
    
    const attrType = prompt('Tipo del atributo (ej: int, String):', 'String');
    if (!attrType || attrType.trim() === '') return;

    this.errorMessage = '';

    this.umlService.addAttribute(this.projectId, cls.id, {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion,
      attributeName: attrName.trim(),
      attributeType: attrType.trim(),
      visibility: 'PRIVATE'
    }).subscribe({
      next: (response) => {
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        // The API returns the added attribute, but we rely on WebSocket or full reload if needed.
        // Actually, we can update local state directly or just let WS handle it.
        // But for ClassCreated we did local update. 
        // For attribute we just append it if not already there, but wait, `upsertClass` only expects class.
        // Let's just reload the model or append locally.
        const clsToUpdate = this.classes.find(c => c.id === cls.id);
        if (clsToUpdate) {
            clsToUpdate.attributes = clsToUpdate.attributes || [];
            clsToUpdate.attributes.push(response.attribute);
            this.upsertClass(clsToUpdate);
        }
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

  private isNewAttribute(classId: string, attributeId: string): boolean {
    const cls = this.classes.find(c => c.id === classId);
    if (!cls || !cls.attributes) return true;
    return cls.attributes.findIndex(a => a.id === attributeId) === -1;
  }

  updateAttribute(cls: UmlClassDto, attr: any): void {
    const newName = prompt(`Editar nombre de ${attr.name}:`, attr.name);
    if (!newName || newName.trim() === '') return;

    const newType = prompt(`Editar tipo de ${newName}:`, attr.type);
    if (!newType || newType.trim() === '') return;

    const newVisibility = prompt(`Editar visibilidad (PUBLIC, PRIVATE, PROTECTED, PACKAGE) de ${newName}:`, attr.visibility);
    if (!newVisibility || newVisibility.trim() === '') return;

    this.errorMessage = '';

    const request = {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion,
      name: newName.trim(),
      type: newType.trim(),
      visibility: newVisibility.trim().toUpperCase()
    };

    this.umlService.updateAttribute(this.projectId, cls.id, attr.id, request).subscribe({
      next: (response) => {
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        const targetClass = this.classes.find(c => c.id === cls.id);
        if (targetClass) {
          targetClass.attributes = targetClass.attributes || [];
          const attrIndex = targetClass.attributes.findIndex(a => a.id === attr.id);
          if (attrIndex !== -1) {
            targetClass.attributes[attrIndex] = response.attribute;
            this.upsertClass(targetClass);
          }
        }
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
          this.loadModel();
        } else if (err.status === 404) {
          this.errorMessage = 'Proyecto, Clase o Atributo no encontrado';
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
