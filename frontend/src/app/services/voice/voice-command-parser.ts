export enum VoiceCommandType {
  CREATE_CLASS = 'CREATE_CLASS',
  RENAME_CLASS = 'RENAME_CLASS',
  ADD_ATTRIBUTE = 'ADD_ATTRIBUTE',
  ADD_OPERATION = 'ADD_OPERATION',
  REMOVE_CLASS = 'REMOVE_CLASS',
  ADD_RELATIONSHIP = 'ADD_RELATIONSHIP',
  UNKNOWN = 'UNKNOWN'
}

export interface VoiceCommand {
  type: VoiceCommandType;
  className?: string;
  newClassName?: string;
  attributeName?: string;
  attributeType?: string;
  operationName?: string;
  sourceClass?: string;
  targetClass?: string;
  relationshipType?: 'ASSOCIATION' | 'COMPOSITION' | 'AGGREGATION' | 'GENERALIZATION';
  sourceMultiplicity?: string;
  targetMultiplicity?: string;
  originalText: string;
  error?: string;
}

export class VoiceCommandParser {
  public parse(text: string): VoiceCommand {
    if (!text || text.trim() === '') {
      return { type: VoiceCommandType.UNKNOWN, originalText: text, error: 'Empty transcript' };
    }
    
    // Normalize spaces
    const normalized = text.trim().replace(/\s+/g, ' ').toLowerCase();
    const originalText = text.trim().replace(/\s+/g, ' ');

    // 1. Create class
    // "crear clase Cliente", "crea clase Pedido", "agregar clase Factura"
    const createClassMatch = normalized.match(/^(?:crear|crea|agregar|agrega|a\u00f1adir|a\u00f1ade)\s+clase\s+([a-z0-9_]+)$/);
    if (createClassMatch) {
      return {
        type: VoiceCommandType.CREATE_CLASS,
        className: this.extractOriginalCase(originalText, createClassMatch[1]),
        originalText
      };
    }

    // 2. Rename class
    // "renombrar clase Cliente a Persona"
    const renameClassMatch = normalized.match(/^(?:renombrar|renombra|cambiar nombre de)\s+clase\s+([a-z0-9_]+)\s+a\s+([a-z0-9_]+)$/);
    if (renameClassMatch) {
      return {
        type: VoiceCommandType.RENAME_CLASS,
        className: this.extractOriginalCase(originalText, renameClassMatch[1]),
        newClassName: this.extractOriginalCase(originalText, renameClassMatch[2]),
        originalText
      };
    }

    // 3. Remove class
    // "eliminar clase Cliente", "borrar clase Pedido"
    const removeClassMatch = normalized.match(/^(?:eliminar|elimina|borrar|borra)\s+clase\s+([a-z0-9_]+)$/);
    if (removeClassMatch) {
      return {
        type: VoiceCommandType.REMOVE_CLASS,
        className: this.extractOriginalCase(originalText, removeClassMatch[1]),
        originalText
      };
    }

    // 4. Add attribute
    // "agregar atributo nombre tipo String a Cliente"
    const addAttrMatch = normalized.match(/^(?:agregar|agrega|a\u00f1adir|a\u00f1ade|crear|crea)\s+atributo\s+([a-z0-9_]+)\s+tipo\s+([a-z0-9_]+)\s+a\s+([a-z0-9_]+)$/);
    if (addAttrMatch) {
      return {
        type: VoiceCommandType.ADD_ATTRIBUTE,
        attributeName: this.extractOriginalCase(originalText, addAttrMatch[1]),
        attributeType: this.extractOriginalCase(originalText, addAttrMatch[2]),
        className: this.extractOriginalCase(originalText, addAttrMatch[3]),
        originalText
      };
    }

    // 5. Add operation
    // "agregar operación calcularTotal a Pedido"
    const addOpMatch = normalized.match(/^(?:agregar|agrega|a\u00f1adir|a\u00f1ade|crear|crea)\s+operaci\u00f3n\s+([a-z0-9_]+)\s+a\s+([a-z0-9_]+)$/);
    if (addOpMatch) {
      return {
        type: VoiceCommandType.ADD_OPERATION,
        operationName: this.extractOriginalCase(originalText, addOpMatch[1]),
        className: this.extractOriginalCase(originalText, addOpMatch[2]),
        originalText
      };
    }

    // 6. Add relationship
    // "crear asociación de Cliente a Pedido"
    // "crear composición de Pedido uno a LineaPedido cero a muchos"
    const relMatch = normalized.match(/^(?:crear|crea|agregar|agrega|a\u00f1adir|a\u00f1ade)\s+(asociaci\u00f3n|composici\u00f3n|agregaci\u00f3n|generalizaci\u00f3n)\s+de\s+([a-z0-9_]+)(?:\s+(uno|muchos|cero a muchos|uno a muchos|cero o uno))?\s+a\s+([a-z0-9_]+)(?:\s+(uno|muchos|cero a muchos|uno a muchos|cero o uno))?$/);
    if (relMatch) {
      let relType: 'ASSOCIATION' | 'COMPOSITION' | 'AGGREGATION' | 'GENERALIZATION' = 'ASSOCIATION';
      if (relMatch[1] === 'composici\u00f3n') relType = 'COMPOSITION';
      else if (relMatch[1] === 'agregaci\u00f3n') relType = 'AGGREGATION';
      else if (relMatch[1] === 'generalizaci\u00f3n') relType = 'GENERALIZATION';

      return {
        type: VoiceCommandType.ADD_RELATIONSHIP,
        relationshipType: relType,
        sourceClass: this.extractOriginalCase(originalText, relMatch[2]),
        sourceMultiplicity: this.parseMultiplicity(relMatch[3]),
        targetClass: this.extractOriginalCase(originalText, relMatch[4]),
        targetMultiplicity: this.parseMultiplicity(relMatch[5]),
        originalText
      };
    }

    return { type: VoiceCommandType.UNKNOWN, originalText, error: 'Command not understood' };
  }

  private extractOriginalCase(originalText: string, lowerCaseWord: string): string {
    const regex = new RegExp(`\\b${lowerCaseWord}\\b`, 'i');
    const match = originalText.match(regex);
    return match ? match[0] : lowerCaseWord;
  }

  private parseMultiplicity(multStr: string | undefined): string | undefined {
    if (!multStr) return undefined;
    switch (multStr) {
      case 'uno': return '1';
      case 'muchos': return '*';
      case 'cero a muchos': return '0..*';
      case 'uno a muchos': return '1..*';
      case 'cero o uno': return '0..1';
      default: return undefined;
    }
  }
}
