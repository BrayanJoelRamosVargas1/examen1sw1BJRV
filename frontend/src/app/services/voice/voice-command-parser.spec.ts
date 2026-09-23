import { VoiceCommandParser, VoiceCommandType } from './voice-command-parser';

describe('VoiceCommandParser', () => {
  let parser: VoiceCommandParser;

  beforeEach(() => {
    parser = new VoiceCommandParser();
  });

  it('should parse CREATE_CLASS', () => {
    const res = parser.parse('crear clase Cliente');
    expect(res.type).toBe(VoiceCommandType.CREATE_CLASS);
    expect(res.className).toBe('Cliente');
  });

  it('should parse CREATE_CLASS with variants and spaces', () => {
    const res = parser.parse('  Añadir   CLASE  Factura  ');
    expect(res.type).toBe(VoiceCommandType.CREATE_CLASS);
    expect(res.className).toBe('Factura');
  });

  it('should parse RENAME_CLASS', () => {
    const res = parser.parse('renombrar clase Cliente a Persona');
    expect(res.type).toBe(VoiceCommandType.RENAME_CLASS);
    expect(res.className).toBe('Cliente');
    expect(res.newClassName).toBe('Persona');
  });

  it('should parse REMOVE_CLASS', () => {
    const res = parser.parse('eliminar clase Cliente');
    expect(res.type).toBe(VoiceCommandType.REMOVE_CLASS);
    expect(res.className).toBe('Cliente');
  });

  it('should parse ADD_ATTRIBUTE', () => {
    const res = parser.parse('agregar atributo nombre tipo String a Cliente');
    expect(res.type).toBe(VoiceCommandType.ADD_ATTRIBUTE);
    expect(res.attributeName).toBe('nombre');
    expect(res.attributeType).toBe('String');
    expect(res.className).toBe('Cliente');
  });

  it('should parse ADD_OPERATION', () => {
    const res = parser.parse('agregar operación calcularTotal a Pedido');
    expect(res.type).toBe(VoiceCommandType.ADD_OPERATION);
    expect(res.operationName).toBe('calcularTotal');
    expect(res.className).toBe('Pedido');
  });

  it('should parse ADD_RELATIONSHIP with multiplicities', () => {
    const res = parser.parse('crear asociación de Cliente uno a Pedido cero a muchos');
    expect(res.type).toBe(VoiceCommandType.ADD_RELATIONSHIP);
    expect(res.relationshipType).toBe('ASSOCIATION');
    expect(res.sourceClass).toBe('Cliente');
    expect(res.sourceMultiplicity).toBe('1');
    expect(res.targetClass).toBe('Pedido');
    expect(res.targetMultiplicity).toBe('0..*');
  });

  it('should parse ADD_RELATIONSHIP without multiplicities', () => {
    const res = parser.parse('crear composición de Pedido a LineaPedido');
    expect(res.type).toBe(VoiceCommandType.ADD_RELATIONSHIP);
    expect(res.relationshipType).toBe('COMPOSITION');
    expect(res.sourceClass).toBe('Pedido');
    expect(res.sourceMultiplicity).toBeUndefined();
    expect(res.targetClass).toBe('LineaPedido');
    expect(res.targetMultiplicity).toBeUndefined();
  });

  it('should handle UNKNOWN command', () => {
    const res = parser.parse('quiero un diagrama de clases nuevo');
    expect(res.type).toBe(VoiceCommandType.UNKNOWN);
    expect(res.error).toBe('Command not understood');
  });

  it('should handle empty input', () => {
    const res = parser.parse('   ');
    expect(res.type).toBe(VoiceCommandType.UNKNOWN);
    expect(res.error).toBe('Empty transcript');
  });
});
