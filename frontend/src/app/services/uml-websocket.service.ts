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

export type WsStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

@Injectable({ providedIn: 'root' })
export class UmlWebSocketService {
  private client!: Client;
  private eventSubject = new Subject<ClassCreatedEvent>();
  private statusSubject = new Subject<WsStatus>();

  /** Stream de eventos CLASS_CREATED recibidos */
  events$: Observable<ClassCreatedEvent> = this.eventSubject.asObservable();
  /** Stream del estado de conexión */
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
              const event: ClassCreatedEvent = JSON.parse(message.body);
              this.eventSubject.next(event);
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
