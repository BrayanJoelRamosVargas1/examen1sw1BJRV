# Fase 12.0: Cliente móvil Flutter

## Alcance MVP

`mobile/` es un cliente Flutter separado de Angular. Usa REST para cargar el modelo, crear clases y agregar atributos. Las mutaciones envían `commandId`, `participantId` móvil y `expectedVersion`.

La configuración se realiza con:

```text
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
flutter run --dart-define=PROJECT_ID=00000000-0000-0000-0000-000000000001
```

`10.0.2.2` es la dirección del host desde Android Emulator. No se hardcodea `localhost` como única opción.

## Realtime y conflictos

`UmlRealtimeService` usa STOMP sobre el endpoint SockJS `/ws-uml`, suscribe el tópico del proyecto e ignora eventos stale. Un gap de versión solicita resync. Un HTTP 409 no se reintenta: se hace GET `/model` y se informa que otro cliente cambió el modelo.

La pantalla MVP muestra versión, conexión, clases, atributos, operaciones y relaciones. Crear clase y agregar atributo reutilizan los endpoints existentes.

## Validación

Flutter está disponible en la máquina:

- `flutter analyze`: sin issues.
- `flutter test`: 5 tests correctos.

Los tests cubren parsing de DTOs, payload de creación con `expectedVersion`, conflicto 409 y eventos realtime stale/gap. Offline y sincronización avanzada quedan para Fase 13.
