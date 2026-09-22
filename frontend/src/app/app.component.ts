import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { UmlService, UmlClassDto } from './services/uml.service';
import { UmlWebSocketService, ClassCreatedEvent, ClassRenamedEvent, UmlEvent, WsStatus, AttributeUpdatedEvent, OperationAddedEvent, OperationRemovedEvent, DiagramEvent } from './services/uml-websocket.service';
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
  relationships: any[] = [];
  eventLog: string[] = [];
  newClassName = '';
  newRelSourceId = '';
  newRelTargetId = '';
  newRelType = 'ASSOCIATION';
  errorMessage = '';

  // Buffer de eventos que llegan antes de que el GET snapshot termine
  private eventBuffer: UmlEvent[] = [];
  private snapshotLoaded = false;
  private snapshotRequestId = 0;
  private wsSub?: Subscription;

  // Estado del layout y variables para el drag
  currentLayoutVersion = 0;
  private layoutEventBuffer: DiagramEvent[] = [];
  private layoutSnapshotLoaded = false;
  private layoutSnapshotRequestId = 0;
  private layoutWsSub?: Subscription;

  nodePositions: Record<string, { x: number; y: number } | undefined> = {};

  draggingClassId: string | null = null;
  dragStartX = 0;
  dragStartY = 0;
  initialNodeX = 0;
  initialNodeY = 0;

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

    this.layoutWsSub = this.wsService.layoutEvents$.subscribe(event => {
      if (!this.layoutSnapshotLoaded) {
        this.layoutEventBuffer.push(event);
      } else {
        this.applyLayoutEvent(event);
      }
    });

    // Paso 1: conectar y suscribirse al topic
    await this.wsService.connect(this.projectId);

    // Paso 2: obtener snapshots iniciales
    this.loadModel();
    this.loadLayout();
  }

  private loadModel(): void {
    const requestId = ++this.snapshotRequestId;
    this.snapshotLoaded = false;
    this.umlService.getModel(this.projectId).subscribe({
      next: (model) => {
        if (requestId !== this.snapshotRequestId) return;
        // Renderizar snapshot
        this.currentVersion = model.version;
        this.classes = [...model.classes];
        this.relationships = model.relationships ? [...model.relationships] : [];
        this.snapshotLoaded = true;

        // Paso 3: aplicar eventos bufferizados usando la misma regla estricta de secuencia
        // (pueden haber llegado por WS mientras el GET estaba en vuelo)
        const bufferedEvents = this.eventBuffer;
        this.eventBuffer = [];
        for (const bufferedEvent of bufferedEvents) {
          if (this.snapshotLoaded) {
            this.applyEvent(bufferedEvent);
          } else {
            this.eventBuffer.push(bufferedEvent);
          }
        }

        this.applyFallbackPositions();
      },
      error: (err) => {
        if (requestId !== this.snapshotRequestId) return;
        this.errorMessage = 'Error cargando el modelo: ' + err.message;
        this.snapshotLoaded = true;
        this.eventBuffer = [];
      }
    });
  }

  private loadLayout(): void {
    const requestId = ++this.layoutSnapshotRequestId;
    this.layoutSnapshotLoaded = false;
    this.umlService.getDiagram(this.projectId).subscribe({
      next: (layout) => {
        if (requestId !== this.layoutSnapshotRequestId) return;
        this.currentLayoutVersion = layout.layoutVersion;

        // Asignar posiciones persistidas
        layout.nodeViews.forEach((nv: any) => {
          this.nodePositions[nv.classId] = { x: nv.x, y: nv.y };
        });

        this.layoutSnapshotLoaded = true;
        this.applyFallbackPositions();

        const bufferedEvents = this.layoutEventBuffer;
        this.layoutEventBuffer = [];
        for (const bufferedEvent of bufferedEvents) {
          if (this.layoutSnapshotLoaded) {
            this.applyLayoutEvent(bufferedEvent);
          } else {
            this.layoutEventBuffer.push(bufferedEvent);
          }
        }
      },
      error: (err) => {
        if (requestId !== this.layoutSnapshotRequestId) return;
        this.errorMessage = 'Error cargando layout: ' + err.message;
        this.layoutSnapshotLoaded = true;
        this.layoutEventBuffer = [];
      }
    });
  }

  private applyFallbackPositions(): void {
    if (!this.snapshotLoaded || !this.layoutSnapshotLoaded) return;

    let index = 0;
    for (const cls of this.classes) {
      if (!this.nodePositions[cls.id]) {
        const col = index % 3;
        const row = Math.floor(index / 3);
        this.nodePositions[cls.id] = { x: 40 + col * 360, y: 40 + row * 360 };
      }
      index++;
    }
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
      this.upsertClass({ id: event.classId, name: event.newName, attributes: targetClass ? targetClass.attributes : [], operations: targetClass ? targetClass.operations : [] });
      this.currentVersion = event.modelVersion;
      this.eventLog.unshift(`CLASS_RENAMED "${event.newName}" v${event.modelVersion}`);
    } else if ('eventType' in event && event.eventType === 'ATTRIBUTE_REMOVED') {
      const targetClass = this.classes.find(c => c.id === event.classId);
      if (!targetClass || !targetClass.attributes?.some(a => a.id === event.attributeId)) {
        this.loadModel();
        return;
      }
      targetClass.attributes = targetClass.attributes.filter(a => a.id !== event.attributeId);
      this.upsertClass(targetClass);
      this.currentVersion = event.modelVersion;
      this.eventLog.unshift(`ATTRIBUTE_REMOVED "${event.attributeId}" v${event.modelVersion}`);
    } else if ('eventType' in event && event.eventType === 'OPERATION_REMOVED') {
      const e = event as OperationRemovedEvent;
      const targetClass = this.classes.find(c => c.id === e.classId);
      if (targetClass && targetClass.operations) {
        const initialLength = targetClass.operations.length;
        targetClass.operations = targetClass.operations.filter(o => o.id !== e.operationId);
        if (targetClass.operations.length < initialLength) {
          this.eventLog.unshift(`OPERATION_REMOVED "${e.operationId}" v${e.modelVersion}`);
          this.upsertClass(targetClass);
          this.currentVersion = e.modelVersion;
        }
      }
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
    } else if ('attributeId' in event && 'name' in event) {
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
    } else if ('eventType' in event && event.eventType === 'OPERATION_ADDED') {
      const targetClass = this.classes.find(c => c.id === event.classId);
      if (targetClass) {
        targetClass.operations = targetClass.operations || [];
        targetClass.operations.push({
          id: event.operationId,
          name: event.name,
          returnType: event.returnType,
          visibility: event.visibility,
          orderIndex: event.orderIndex,
          parameters: event.parameters
        });
        this.eventLog.unshift(`OPERATION_ADDED "${event.name}" v${event.modelVersion}`);
        this.upsertClass(targetClass);
        this.currentVersion = event.modelVersion;
      } else {
        this.loadModel();
        return;
      }
    } else if ('eventType' in event && event.eventType === 'OPERATION_UPDATED') {
      const targetClass = this.classes.find(c => c.id === event.classId);
      if (targetClass) {
        targetClass.operations = targetClass.operations || [];
        const opIndex = targetClass.operations.findIndex(o => o.id === event.operationId);

        if (opIndex !== -1) {
          targetClass.operations[opIndex] = {
            id: event.operationId,
            name: event.name,
            returnType: event.returnType,
            visibility: event.visibility,
            orderIndex: event.orderIndex,
            parameters: event.parameters
          };
          this.eventLog.unshift(`OPERATION_UPDATED "${event.name}" v${event.modelVersion}`);
          this.upsertClass(targetClass);
          this.currentVersion = event.modelVersion;
        } else {
          this.loadModel();
          return;
        }
      } else {
        this.loadModel();
        return;
      }
    } else if ('className' in event) {
      // Es ClassCreatedEvent
      this.upsertClass({ id: event.classId, name: event.className, attributes: [], operations: [] });
      this.currentVersion = event.modelVersion;
      this.eventLog.unshift(`CLASS_CREATED "${event.className}" v${event.modelVersion}`);
    } else if ('eventType' in event && event.eventType === 'RELATIONSHIP_ADDED') {
      const e = event as any;
      const exists = this.relationships.some(r => r.id === e.relationshipId);
      if (!exists) {
        this.relationships.push({
          id: e.relationshipId,
          type: e.relationshipType,
          sourceClassId: e.sourceClassId,
          targetClassId: e.targetClassId,
          sourceMultiplicity: e.sourceMultiplicity,
          targetMultiplicity: e.targetMultiplicity
        });
        this.eventLog.unshift(`RELATIONSHIP_ADDED v${e.modelVersion}`);
        this.currentVersion = e.modelVersion;
      }
    } else if ('eventType' in event && event.eventType === 'RELATIONSHIP_UPDATED') {
      const e = event as any;
      const index = this.relationships.findIndex(r => r.id === e.relationshipId);
      if (index !== -1) {
        this.relationships[index].type = e.type;
        this.relationships[index].sourceMultiplicity = e.sourceMultiplicity;
        this.relationships[index].targetMultiplicity = e.targetMultiplicity;
        this.eventLog.unshift(`RELATIONSHIP_UPDATED v${e.modelVersion}`);
        this.currentVersion = e.modelVersion;
      } else {
        this.loadModel();
        return;
      }
    } else {
      this.loadModel();
      return;
    }

    // Mantener log acotado
    if (this.eventLog.length > 20) {
      this.eventLog.pop();
    }
  }

  private applyLayoutEvent(event: DiagramEvent): void {
    if (event.layoutVersion <= this.currentLayoutVersion) {
      return;
    }

    if (event.layoutVersion > this.currentLayoutVersion + 1) {
      this.loadLayout();
      return;
    }

    this.nodePositions[event.classId] = { x: event.x, y: event.y };
    this.currentLayoutVersion = event.layoutVersion;
    this.eventLog.unshift(`NODE_MOVED v${event.layoutVersion}`);
    if (this.eventLog.length > 20) this.eventLog.pop();
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
        this.upsertClass({ id: response.classId, name: response.newName, attributes: existing ? existing.attributes : [], operations: existing ? existing.operations : [] });
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
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > this.currentVersion + 1) {
          this.loadModel();
          return;
        }
        if (response.modelVersion < this.currentVersion) {
          if (!this.classes.find(c => c.id === cls.id)?.attributes?.some(a => a.id === response.attribute.id)) {
            this.loadModel();
          }
          return;
        }
        const clsToUpdate = this.classes.find(c => c.id === cls.id);
        if (!clsToUpdate) {
          this.loadModel();
          return;
        }
        clsToUpdate.attributes = clsToUpdate.attributes || [];
        if (!clsToUpdate.attributes.some(a => a.id === response.attribute.id)) {
            clsToUpdate.attributes.push(response.attribute);
        }
        this.upsertClass(clsToUpdate);
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
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
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > this.currentVersion + 1) {
          this.loadModel();
          return;
        }
        if (response.modelVersion < this.currentVersion) {
          this.loadModel();
          return;
        }
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

  removeAttribute(cls: UmlClassDto, attr: any): void {
    if (!confirm(`¿Eliminar el atributo ${attr.name} de ${cls.name}?`)) return;
    this.errorMessage = '';

    this.umlService.removeAttribute(this.projectId, cls.id, attr.id, {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion
    }).subscribe({
      next: (response) => {
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > this.currentVersion + 1) {
          this.loadModel();
          return;
        }
        if (response.modelVersion < this.currentVersion) {
          this.loadModel();
          return;
        }
        const alreadyApplied = this.currentVersion >= response.modelVersion;
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        const targetClass = this.classes.find(c => c.id === response.classId);
        if (!targetClass || !targetClass.attributes?.some(a => a.id === response.attributeId)) {
          if (!alreadyApplied) this.loadModel();
          return;
        }
        targetClass.attributes = targetClass.attributes.filter(a => a.id !== response.attributeId);
        this.upsertClass(targetClass);
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
          this.loadModel();
        } else {
          this.errorMessage = 'Error eliminando atributo: ' + (err.error?.error || err.message);
        }
      }
    });
  }

  addOperation(cls: UmlClassDto): void {
    const opName = prompt('Nombre de la nueva operación:');
    if (!opName || opName.trim() === '') return;

    const opReturnType = prompt('Tipo de retorno (ej: void, String, int):', 'void');
    if (!opReturnType || opReturnType.trim() === '') return;

    // Pedir visibilidad explícitamente — requerida por el contrato AddOperationRequest
    const validVisibilities = ['PUBLIC', 'PRIVATE', 'PROTECTED', 'PACKAGE'];
    let opVisibility = prompt('Visibilidad (PUBLIC, PRIVATE, PROTECTED, PACKAGE):', 'PUBLIC');
    if (!opVisibility || opVisibility.trim() === '') return;
    opVisibility = opVisibility.trim().toUpperCase();
    if (!validVisibilities.includes(opVisibility)) {
      this.errorMessage = `Visibilidad inválida: "${opVisibility}". Use PUBLIC, PRIVATE, PROTECTED o PACKAGE.`;
      return;
    }

    // Captura de parámetros con numeración para que el usuario sepa cuándo puede parar
    const parameters: { name: string, type: string }[] = [];
    let paramIndex = 1;
    while (true) {
      const paramName = prompt(
        `Parámetro #${paramIndex} — nombre\n(deja vacío y pulsa Aceptar para finalizar):`);
      if (!paramName || paramName.trim() === '') break;
      const paramType = prompt(`Parámetro #${paramIndex} — tipo de "${paramName.trim()}":`, 'String');
      if (!paramType || paramType.trim() === '') break;
      parameters.push({ name: paramName.trim(), type: paramType.trim() });
      paramIndex++;
    }

    this.errorMessage = '';

    this.umlService.addOperation(this.projectId, cls.id, {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion,
      name: opName.trim(),
      returnType: opReturnType.trim(),
      visibility: opVisibility,
      parameters
    }).subscribe({
      next: (response) => {
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > this.currentVersion + 1) {
          this.loadModel();
          return;
        }
        if (response.modelVersion < this.currentVersion) {
          if (!this.classes.find(c => c.id === cls.id)?.operations?.some(o => o.id === response.operationId)) {
            this.loadModel();
          }
          return;
        }
        const clsToUpdate = this.classes.find(c => c.id === cls.id);
        if (!clsToUpdate) {
          this.loadModel();
          return;
        }
        clsToUpdate.operations = clsToUpdate.operations || [];
        if (!clsToUpdate.operations.some(o => o.id === response.operationId)) {
            clsToUpdate.operations.push({
              id: response.operationId,
              name: response.name,
              returnType: response.returnType,
              visibility: response.visibility,
              orderIndex: response.orderIndex,
              parameters: response.parameters
            });
        }
        this.upsertClass(clsToUpdate);
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
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

  updateOperation(cls: UmlClassDto, op: any): void {
    const opName = prompt('Editar nombre de la operación:', op.name);
    if (!opName || opName.trim() === '') return;

    const opReturnType = prompt(`Editar tipo de retorno de ${opName.trim()}:`, op.returnType);
    if (!opReturnType || opReturnType.trim() === '') return;

    const validVisibilities = ['PUBLIC', 'PRIVATE', 'PROTECTED', 'PACKAGE'];
    let opVisibility = prompt(`Editar visibilidad (PUBLIC, PRIVATE, PROTECTED, PACKAGE) de ${opName.trim()}:`, op.visibility);
    if (!opVisibility || opVisibility.trim() === '') return;
    opVisibility = opVisibility.trim().toUpperCase();
    if (!validVisibilities.includes(opVisibility)) {
      this.errorMessage = `Visibilidad inválida: "${opVisibility}".`;
      return;
    }

    const parameters: { id: string | null, name: string, type: string }[] = [];

    let paramIndex = 1;
    for (const existingParam of (op.parameters || [])) {
      const paramName = prompt(
        `Editar parámetro #${paramIndex} (anteriormente ${existingParam.name}: ${existingParam.type}) — nombre\n(deja vacío para eliminar este parámetro y detener la precarga):`, existingParam.name);

      if (!paramName || paramName.trim() === '') {
          break;
      }

      const paramType = prompt(`Editar tipo de "${paramName.trim()}":`, existingParam.type);
      if (!paramType || paramType.trim() === '') break;

      parameters.push({ id: existingParam.id, name: paramName.trim(), type: paramType.trim() });
      paramIndex++;
    }

    while (true) {
      const paramName = prompt(
        `Parámetro #${paramIndex} (NUEVO) — nombre\n(deja vacío y pulsa Aceptar para finalizar):`);
      if (!paramName || paramName.trim() === '') break;
      const paramType = prompt(`Parámetro #${paramIndex} — tipo de "${paramName.trim()}":`, 'String');
      if (!paramType || paramType.trim() === '') break;
      parameters.push({ id: null, name: paramName.trim(), type: paramType.trim() });
      paramIndex++;
    }

    this.errorMessage = '';

    this.umlService.updateOperation(this.projectId, cls.id, op.id, {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion,
      name: opName.trim(),
      returnType: opReturnType.trim(),
      visibility: opVisibility,
      parameters
    }).subscribe({
      next: (response) => {
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > this.currentVersion + 1) {
          this.loadModel();
          return;
        }
        if (response.modelVersion < this.currentVersion) {
          this.loadModel();
          return;
        }
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        const targetClass = this.classes.find(c => c.id === cls.id);
        if (targetClass) {
          targetClass.operations = targetClass.operations || [];
          const index = targetClass.operations.findIndex(o => o.id === op.id);
          if (index !== -1) {
            targetClass.operations[index] = {
              id: response.operationId,
              name: response.name,
              returnType: response.returnType,
              visibility: response.visibility,
              orderIndex: response.orderIndex,
              parameters: response.parameters
            };
            this.upsertClass(targetClass);
          }
        }
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
          this.loadModel();
        } else if (err.status === 404) {
          this.errorMessage = 'Proyecto, Clase u Operación no encontrado';
        } else {
          this.errorMessage = 'Error: ' + (err.error?.error || err.message);
        }
      }
    });
  }

  /** Convierte el enum Visibility al símbolo UML estándar */
  visibilitySymbol(visibility: string): string {
    switch (visibility?.toUpperCase()) {
      case 'PUBLIC':    return '+';
      case 'PRIVATE':   return '-';
      case 'PROTECTED': return '#';
      case 'PACKAGE':   return '~';
      default:          return '?';
    }
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

    // Asignar posición default si es nueva
    if (!this.nodePositions[cls.id]) {
       this.nodePositions[cls.id] = { x: 40, y: 40 };
       this.applyFallbackPositions(); // reacomodar las default si se quiere
    }
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.layoutWsSub?.unsubscribe();
    this.wsService.disconnect();
  }

  removeOperation(cls: any, op: any) {
    if (confirm(`¿Eliminar operación "${op.name}"?`)) {
      this.umlService.deleteOperation(this.projectId, cls.id, op.id, {
        commandId: crypto.randomUUID(),
        participantId: 'browser-A',
        expectedVersion: this.currentVersion
      }).subscribe({
        next: (response) => {
          this.currentVersion = response.modelVersion;
          cls.operations = cls.operations.filter((o: any) => o.id !== op.id);
          this.upsertClass(cls);
        },
        error: (err) => {
          if (err.status === 409) {
            this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
            this.loadModel();
          } else {
            this.errorMessage = 'Error: ' + (err.error?.error || err.message);
          }
        }
      });
    }
  }

  updateRelationship(rel: any): void {
    const newType = prompt('Editar tipo (ASSOCIATION, AGGREGATION, COMPOSITION, GENERALIZATION, REALIZATION, DEPENDENCY):', rel.type);
    if (!newType || newType.trim() === '') return;

    let sourceMult = rel.sourceMultiplicity || '';
    let targetMult = rel.targetMultiplicity || '';

    if (newType === 'ASSOCIATION' || newType === 'AGGREGATION' || newType === 'COMPOSITION') {
      sourceMult = prompt(`Editar multiplicidad origen (actual: ${sourceMult}). Deja vacío para omitir:`, sourceMult) || '';
      targetMult = prompt(`Editar multiplicidad destino (actual: ${targetMult}). Deja vacío para omitir:`, targetMult) || '';
    } else {
      sourceMult = '';
      targetMult = '';
    }

    this.errorMessage = '';

    this.umlService.updateRelationship(this.projectId, rel.id, {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion,
      type: newType.trim().toUpperCase(),
      sourceMultiplicity: sourceMult,
      targetMultiplicity: targetMult
    }).subscribe({
      next: (response) => {
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > this.currentVersion + 1) {
          this.loadModel();
          return;
        }
        if (response.modelVersion < this.currentVersion) {
          this.loadModel();
          return;
        }
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        const index = this.relationships.findIndex(r => r.id === rel.id);
        if (index !== -1) {
          this.relationships[index].type = response.type;
          this.relationships[index].sourceMultiplicity = response.sourceMultiplicity;
          this.relationships[index].targetMultiplicity = response.targetMultiplicity;
        }
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
          this.loadModel();
        } else {
          this.errorMessage = 'Error: ' + (err.error?.error || err.message);
        }
      }
    });
  }

  addRelationship(): void {
    if (!this.newRelSourceId || !this.newRelTargetId || this.newRelSourceId === this.newRelTargetId) return;

    let sourceMult = '';
    let targetMult = '';

    if (this.newRelType === 'ASSOCIATION' || this.newRelType === 'AGGREGATION' || this.newRelType === 'COMPOSITION') {
      sourceMult = prompt('Multiplicidad origen (ej: 1, 0..1, *). Deja vacío para omitir:') || '';
      targetMult = prompt('Multiplicidad destino (ej: 1, 0..1, *). Deja vacío para omitir:') || '';
    }

    this.errorMessage = '';

    this.umlService.addRelationship(this.projectId, {
      commandId: uuidv4(),
      participantId: 'browser-A',
      expectedVersion: this.currentVersion,
      type: this.newRelType,
      sourceClassId: this.newRelSourceId,
      targetClassId: this.newRelTargetId,
      sourceMultiplicity: sourceMult,
      targetMultiplicity: targetMult
    }).subscribe({
      next: (response) => {
        if (!Number.isSafeInteger(response.modelVersion) || response.modelVersion > this.currentVersion + 1) {
          this.loadModel();
          return;
        }
        if (response.modelVersion < this.currentVersion) {
          if (!this.relationships.some(r => r.id === response.relationshipId)) {
            this.loadModel();
          }
          return;
        }

        if (!this.relationships.some(r => r.id === response.relationshipId)) {
          this.relationships.push(response.relationship);
        }
        this.currentVersion = Math.max(this.currentVersion, response.modelVersion);
        this.newRelSourceId = '';
        this.newRelTargetId = '';
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = 'Conflicto de versión detectado. Resincronizando estado automáticamente...';
          this.loadModel();
        } else {
          this.errorMessage = 'Error: ' + (err.error?.error || err.message);
        }
      }
    });
  }

  getClassName(id: string): string {
    return this.classes.find(c => c.id === id)?.name || id;
  }

  onPointerDown(event: PointerEvent, clsId: string): void {
    if (event.button !== 0) return;
    const target = event.target as HTMLElement;

    // Solo iniciar drag desde la cabecera
    if (!target.closest('.class-card-header')) return;

    this.draggingClassId = clsId;
    this.dragStartX = event.clientX;
    this.dragStartY = event.clientY;

    const pos = this.nodePositions[clsId] || { x: 40, y: 40 };
    this.initialNodeX = pos.x;
    this.initialNodeY = pos.y;

    target.setPointerCapture(event.pointerId);
    event.preventDefault();
  }

  onPointerMove(event: PointerEvent): void {
    if (!this.draggingClassId) return;

    const dx = event.clientX - this.dragStartX;
    const dy = event.clientY - this.dragStartY;

    this.nodePositions[this.draggingClassId] = {
      x: this.initialNodeX + dx,
      y: this.initialNodeY + dy
    };
  }

  onPointerUp(event: PointerEvent): void {
    if (!this.draggingClassId) return;

    const clsId = this.draggingClassId;
    const finalPos = this.nodePositions[clsId];
    this.draggingClassId = null;

    const target = event.target as HTMLElement;
    target.releasePointerCapture(event.pointerId);

    if (!finalPos) return;

    this.umlService.saveNodeView(
      this.projectId,
      clsId,
      uuidv4(),
      'browser-A',
      this.currentLayoutVersion,
      finalPos.x,
      finalPos.y
    ).subscribe({
      next: (response) => {
        this.currentLayoutVersion = Math.max(this.currentLayoutVersion, response.layoutVersion);
        this.nodePositions[clsId] = { x: response.x, y: response.y };
      },
      error: (err) => {
        if (err.status === 409) {
           this.errorMessage = 'Conflicto de layout detectado. Resincronizando posiciones...';
           this.loadLayout();
        } else {
           this.errorMessage = 'Error moviendo nodo: ' + (err.error?.error || err.message);
        }
      }
    });
  }
}
