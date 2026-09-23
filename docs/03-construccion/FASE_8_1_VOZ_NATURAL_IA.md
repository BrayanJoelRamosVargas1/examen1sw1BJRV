# Fase 8.1: Voz natural e interpretacion IA

## Alcance

La fase 8.1 agrega interpretacion de lenguaje natural sin reemplazar el parser determinista de 8.0:

1. El transcript pasa primero por `VoiceCommandParser`.
2. Un comando reconocido se muestra en preview y no llama a IA.
3. Solo un resultado `UNKNOWN` usa `POST /api/projects/{projectId}/ai/interpret`.
4. La IA devuelve comandos UML estructurados, nunca codigo ni instrucciones ejecutables.
5. El usuario confirma el preview antes de ejecutar mutaciones.

La ejecucion usa los mismos casos de uso REST, `expectedVersion`, PostgreSQL y STOMP que la interfaz manual.

## Backend

`AiCommandInterpreter` es el puerto de aplicacion. `OpenAiCompatibleCommandInterpreter` es un adaptador sustituible por otro proveedor compatible o una implementacion local.

La configuracion se realiza exclusivamente mediante variables de entorno:

```text
AI_ENABLED=false
AI_BASE_URL=
AI_API_KEY=
AI_MODEL=
AI_TIMEOUT_SECONDS=15
```

La clave nunca se expone en Angular ni se registra. Con `AI_ENABLED=false`, el parser determinista sigue funcionando.

El endpoint es de solo lectura: carga el modelo actual, entrega al proveedor solo nombres de clases y restricciones UML, y no incrementa version ni publica eventos.

## Contrato y validacion

El resultado esperado tiene esta forma:

```json
{
  "commands": [
    {
      "type": "ADD_ATTRIBUTE",
      "className": "Cliente",
      "attributeName": "nombre",
      "attributeType": "String"
    }
  ]
}
```

Se aceptan `CREATE_CLASS`, `RENAME_CLASS`, `ADD_ATTRIBUTE`, `ADD_OPERATION` y `ADD_RELATIONSHIP`. Los tipos, relaciones y multiplicidades se validan mediante whitelist. Se rechazan comandos incompletos, referencias a clases inexistentes, JSON invalido y lotes de mas de 10 comandos. Las referencias a clases creadas antes en el mismo lote se resuelven por nombre después de cada mutacion.

## Ejecucion del lote

Los comandos confirmados se ejecutan secuencialmente. Cada respuesta actualiza `expectedVersion` antes del siguiente comando. Un `409` detiene el lote, solicita nuevamente el modelo y muestra el conflicto. Cancelar el preview ejecuta cero mutaciones.

## Evidencia y limites

Los tests backend usan un fake/stub de `AiCommandInterpreter` y no consumen una API real. El fake provider esta validado mediante tests unitarios, de API HTTP y de ejecucion secuencial del lote. La integracion tecnica esta implementada.

**Validacion con proveedor IA real: PENDIENTE.**

La validacion fisica del microfono de 8.0 tambien permanece pendiente de una demo manual con microfono real; los tests con mocks no prueban el hardware.
