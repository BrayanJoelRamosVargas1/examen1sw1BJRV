package com.umlcase.infrastructure.interchange.xmi;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Mapeador provisional de tipos primitivos.
 * [PENDIENTE VALIDACIÓN EA REAL]
 */
public class PrimitiveTypeMapper {

    /**
     * Aplica el tipo a un elemento UML (como Property o Parameter).
     */
    public void applyType(Document doc, Element element, String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return;
        }

        // Representación provisional: usar un <type> anidado
        Element typeNode = doc.createElement("type");
        typeNode.setAttribute("xmi:type", "uml:PrimitiveType");
        
        // Mapeo básico
        String mappedType = typeStr;
        switch (typeStr.toLowerCase()) {
            case "string":
                mappedType = "String";
                break;
            case "integer":
            case "int":
                mappedType = "Integer";
                break;
            case "boolean":
            case "bool":
                mappedType = "Boolean";
                break;
            case "decimal":
            case "double":
            case "float":
                mappedType = "Decimal";
                break;
            case "void":
                mappedType = "void";
                break;
            default:
                // Si no es un primitivo conocido, lo dejamos tal cual
                typeNode.setAttribute("xmi:type", "uml:DataType"); // O un tipo interno si estuviera soportado
                break;
        }
        
        typeNode.setAttribute("href", "http://schema.omg.org/spec/UML/2.1/uml.xml#" + mappedType);
        element.appendChild(typeNode);
    }
}
