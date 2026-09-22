import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';

export interface ClassCreatedEvent {
  commandId: string;
  projectId: string;
  classId: string;
  className: string;
  modelVersion: number;
}

export interface ClassRenamedEvent {
  commandId: string;
  projectId: string;
  classId: string;
  newName: string;
  modelVersion: number;
}

export interface AttributeAddedEvent {
  eventType?: string;
  eventId: string;
  commandId: string;
  projectId: string;
  modelVersion: number;
  classId: string;
  attributeId: string;
  name: string;
  type: string;
  visibility: string;
  orderIndex: number;
}

export interface AttributeUpdatedEvent {
  eventType?: string;
  eventId: string;
  commandId: string;
  projectId: string;
  modelVersion: number;
  classId: string;
  attributeId: string;
  name: string;
  type: string;
  visibility: string;
  orderIndex: number;
}

export interface AttributeRemovedEvent {
  eventType: 'ATTRIBUTE_REMOVED';
  commandId: string;
  projectId: string;
  classId: string;
  attributeId: string;
  modelVersion: number;
}

export interface OperationAddedEvent {
  eventType: 'OPERATION_ADDED';
  commandId: string;
  projectId: string;
  classId: string;
  operationId: string;
  name: string;
  returnType: string;
  visibility: string;
  orderIndex: number;
  parameters: {
    id: string;
    name: string;
    type: string;
    orderIndex: number;
  }[];
  modelVersion: number;
}

export interface OperationUpdatedEvent {
  eventType: 'OPERATION_UPDATED';
  commandId: string;
  projectId: string;
  classId: string;
  operationId: string;
  name: string;
  returnType: string;
  visibility: string;
  orderIndex: number;
  parameters: {
    id: string;
    name: string;
    type: string;
    orderIndex: number;
  }[];
  modelVersion: number;
}

export interface OperationRemovedEvent {
  eventType: 'OPERATION_REMOVED';
  commandId: string;
  projectId: string;
  classId: string;
  operationId: string;
  modelVersion: number;
}

export interface NodeMovedEvent {
  eventType: 'NODE_MOVED';
  commandId: string;
  projectId: string;
  classId: string;
  x: number;
  y: number;
  layoutVersion: number;
}

export interface RelationshipAddedEvent {
  eventType: 'RELATIONSHIP_ADDED';
  commandId: string;
  projectId: string;
  relationshipId: string;
  sourceClassId: string;
  targetClassId: string;
  relationshipType: string;
  sourceMultiplicity: string;
  targetMultiplicity: string;
  modelVersion: number;
}

export type UmlEvent =
  | ClassCreatedEvent
  | ClassRenamedEvent
  | AttributeAddedEvent
  | AttributeUpdatedEvent
  | AttributeRemovedEvent
  | OperationAddedEvent
  | OperationUpdatedEvent
  | OperationRemovedEvent
  | RelationshipAddedEvent;

export type DiagramEvent = NodeMovedEvent;

export type WsStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

@Injectable({ providedIn: 'root' })
export class UmlWebSocketService {
  private client!: Client;
  private eventSubject = new Subject<UmlEvent>();
  private layoutEventSubject = new Subject<DiagramEvent>();
  private statusSubject = new Subject<WsStatus>();

  /** Stream de eventos recibidos */
  events$: Observable<UmlEvent> = this.eventSubject.asObservable();
  /** Stream de eventos de layout recibidos */
  layoutEvents$: Observable<DiagramEvent> = this.layoutEventSubject.asObservable();
  /** Stream del estado de conexin */
  status$: Observable<WsStatus> = this.statusSubject.asObservable();

  /**
   * Conecta al broker STOMP y se suscribe al topic del proyecto.
   *
   * ORDEN DE ARRANQUE OBLIGATORIO:
   *   1. connect() → suscribirse al topic
   *   2. GET snapshot
   *   3. renderizar snapshot
   *   4. procesar eventos del buffer si llegaron antes que el GET
   *
   * Retorna una Promise que se resuelve cuando la conexión está establecida.
   */
  connect(projectId: string): Promise<void> {
    return new Promise((resolve) => {
      this.statusSubject.next('connecting');

      this.client = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws-uml'),
        reconnectDelay: 5000,

        onConnect: () => {
          this.statusSubject.next('connected');
          this.client.subscribe(
            `/topic/projects/${projectId}`,
            (message: IMessage) => {
              const event: UmlEvent = JSON.parse(message.body);
              this.eventSubject.next(event);
            }
          );
          this.client.subscribe(
            `/topic/projects/${projectId}/diagram`,
            (message: IMessage) => {
              const event: DiagramEvent = JSON.parse(message.body);
              this.layoutEventSubject.next(event);
            }
          );
          resolve();
        },

        onDisconnect: () => {
          this.statusSubject.next('disconnected');
        },

        onStompError: () => {
          this.statusSubject.next('error');
        },
      });

      this.client.activate();
    });
  }

  disconnect(): void {
    if (this.client?.active) {
      this.client.deactivate();
    }
  }
}
