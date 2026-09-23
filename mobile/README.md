# UML CASE Mobile MVP

Cliente Flutter de Fase 12.0 para consultar y editar el modelo UML mediante REST.

## Ejecutar

```powershell
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
```

`API_BASE_URL` y `PROJECT_ID` son configurables con `--dart-define`. Para Android Emulator, `10.0.2.2` apunta al host; en un dispositivo físico debe usarse la IP accesible de la máquina.

## Alcance

- GET del modelo y visualización de clases, atributos, operaciones y relaciones.
- Crear clase y agregar atributo con `expectedVersion` y `participantId` móvil.
- HTTP 409 fuerza GET de resincronización y no reintenta automáticamente.
- Cliente WebSocket mínimo con ignorado de eventos stale y resync ante gaps.

Offline, sincronización avanzada, edición de operaciones/relaciones y canvas móvil quedan para Fase 13.
