# Fase 8.0: Modelado UML por Voz

## Objetivo

Añadir una interfaz de entrada basada en voz para el modelo UML, permitiendo crear, renombrar y relacionar clases mediante comandos hablados.

## Decisiones de Arquitectura

1. **Reconocimiento de Voz MVP**:
   - Utilizamos la **Web Speech API** (`SpeechRecognition` / `webkitSpeechRecognition`) nativa del navegador como capa MVP.
   - El uso de la API se abstrae completamente detrás de `SpeechRecognitionService`, evitando depender de implementaciones específicas del navegador en el resto de la aplicación.
   - Esto permite un futuro reemplazo (por ej. modelo Whisper o IA local) sin modificar la UI o el Parser.

2. **Parser Determinista (`VoiceCommandParser`)**:
   - Sin NLP complejo ni IA: se usa un parser determinista basado en expresiones regulares para entender una gramática explícita (ej. "crear clase [Nombre]", "agregar atributo [Nombre] tipo [Tipo] a [Clase]").
   - Mapea un `transcript` textual a una estructura interna clara (`VoiceCommand`).
   - Soporte para variantes naturales pero precisas ("crear/crea", "agregar/añadir", etc.).

3. **Reutilización del Flujo Principal**:
   - Los comandos de voz **NO mutan el estado directamente** ni se comunican por vías alternas.
   - Se conectan a las mismas funciones del `AppComponent` / `UmlService` que la UI manual (ej. `createClass()`, `addAttribute()`).
   - **Garantías mantenidas**: El control de concurrencia optimista (`@Version`, `expectedVersion`), la resincronización por conflictos (HTTP 409), el broadcasting STOMP (vía `AFTER_COMMIT` en JPA) y la persistencia en PostgreSQL funcionan exactamente igual que con clics.

## Comandos Soportados

- **Crear Clase**: "crear clase [Nombre]"
- **Renombrar Clase**: "renombrar clase [Viejo] a [Nuevo]"
- **Atributos**: "agregar atributo [Nombre] tipo [Tipo] a [Clase]"
- **Operaciones**: "agregar operación [Nombre] a [Clase]"
- **Eliminar Clase**: "eliminar clase [Nombre]" (Parcialmente manejado a nivel parser).
- **Relaciones**: "crear asociación|composición|agregación|generalización de [Origen] a [Destino]" con soporte opcional para multiplicidades como "uno", "muchos", "cero a muchos", "uno a muchos", "cero o uno".

## Prevención de Errores
- La transcripción se muestra siempre.
- El usuario debe **Aceptar** el preview del comando interpretado antes de mutar el sistema (evitando errores por mala captura del audio).
- Si hay ambigüedad (dos clases con nombre similar) o el comando es desconocido, el sistema lo rechaza y muestra el error.

## Evidencia Manual y Tests

Dado que la Web Speech API requiere permiso de micrófono e interacción del usuario en un entorno de navegador compatible (Chrome/Edge), se incluyen:
1. **Unit tests (`voice-command-parser.spec.ts`)**: Prueban todas las combinaciones de gramática independientemente del navegador.
2. **Unit tests (`speech-recognition.service.spec.ts`)**: Validan el ciclo de vida de la API nativa y el control de errores usando mocks.
3. **Validación real**: Requiere lanzar `npm start` en un navegador, dar permisos de micrófono y comprobar cómo la transcripción fluye por el backend hacia los suscriptores STOMP.
