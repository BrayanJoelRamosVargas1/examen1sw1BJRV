package com.umlcase.infrastructure.interchange.xmi;

import com.umlcase.application.port.out.UmlInterchangeExporter;
import com.umlcase.domain.model.*;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

@Component
public class XmiExportAdapter implements UmlInterchangeExporter {

    private final XmiIdMapper idMapper = new XmiIdMapper();
    private final MultiplicityParser multiplicityParser = new MultiplicityParser();
    private final PrimitiveTypeMapper typeMapper = new PrimitiveTypeMapper();

    @Override
    public byte[] export(UmlModel model) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            // Safe settings for export, though it's mainly for parsing
            docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Root element
            Element rootElement = doc.createElement("xmi:XMI");
            rootElement.setAttribute("xmlns:xmi", "http://schema.omg.org/spec/XMI/2.1");
            rootElement.setAttribute("xmlns:uml", "http://schema.omg.org/spec/UML/2.1");
            rootElement.setAttribute("xmi:version", "2.1");
            doc.appendChild(rootElement);

            // Model element
            Element umlModel = doc.createElement("uml:Model");
            umlModel.setAttribute("xmi:type", "uml:Model");
            umlModel.setAttribute("name", "UmlModelExport");
            umlModel.setAttribute("xmi:id", "EAID_UmlModelExport");
            rootElement.appendChild(umlModel);

            // Export classes — sorted by UUID for deterministic output
            List<UmlClass> sortedClasses = model.getClasses().stream()
                    .sorted(Comparator.comparing(c -> c.getId().toString()))
                    .toList();
            for (UmlClass cls : sortedClasses) {
                exportClass(doc, umlModel, cls);
            }

            // Export relationships — sorted by UUID for deterministic output
            List<UmlRelationship> sortedRelationships = model.getRelationships().stream()
                    .sorted(Comparator.comparing(r -> r.getId().toString()))
                    .toList();
            for (UmlRelationship rel : sortedRelationships) {
                exportRelationship(doc, umlModel, rel);
            }

            // Transform to XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(out);

            transformer.transform(source, result);

            return out.toByteArray();

        } catch (ParserConfigurationException | TransformerException e) {
            throw new RuntimeException("Error exporting model to XMI", e);
        }
    }

    private void exportClass(Document doc, Element parent, UmlClass cls) {
        Element classElement = doc.createElement("packagedElement");
        classElement.setAttribute("xmi:type", "uml:Class");
        classElement.setAttribute("xmi:id", idMapper.toXmiId(cls.getId()));
        classElement.setAttribute("name", cls.getName());

        // Attributes
        for (UmlAttribute attr : cls.getAttributes()) {
            exportAttribute(doc, classElement, attr);
        }

        // Operations
        for (UmlOperation op : cls.getOperations()) {
            exportOperation(doc, classElement, op);
        }

        parent.appendChild(classElement);
    }

    private void exportAttribute(Document doc, Element parentClass, UmlAttribute attr) {
        Element attrElement = doc.createElement("ownedAttribute");
        attrElement.setAttribute("xmi:type", "uml:Property");
        attrElement.setAttribute("xmi:id", idMapper.toXmiId(attr.getId()));
        attrElement.setAttribute("name", attr.getName());
        attrElement.setAttribute("visibility", mapVisibility(attr.getVisibility()));

        typeMapper.applyType(doc, attrElement, attr.getType());

        parentClass.appendChild(attrElement);
    }

    private void exportOperation(Document doc, Element parentClass, UmlOperation op) {
        Element opElement = doc.createElement("ownedOperation");
        opElement.setAttribute("xmi:type", "uml:Operation");
        opElement.setAttribute("xmi:id", idMapper.toXmiId(op.getId()));
        opElement.setAttribute("name", op.getName());
        opElement.setAttribute("visibility", mapVisibility(op.getVisibility()));

        // Return type parameter
        if (op.getReturnType() != null && !op.getReturnType().trim().isEmpty()) {
            Element returnElement = doc.createElement("ownedParameter");
            returnElement.setAttribute("xmi:type", "uml:Parameter");
            returnElement.setAttribute("xmi:id", idMapper.toXmiId(op.getId()) + "_return");
            returnElement.setAttribute("direction", "return");
            typeMapper.applyType(doc, returnElement, op.getReturnType());
            opElement.appendChild(returnElement);
        }

        // Parameters
        for (UmlParameter param : op.getParameters()) {
            Element paramElement = doc.createElement("ownedParameter");
            paramElement.setAttribute("xmi:type", "uml:Parameter");
            paramElement.setAttribute("xmi:id", idMapper.toXmiId(param.getId()));
            paramElement.setAttribute("name", param.getName());
            paramElement.setAttribute("direction", "in");
            typeMapper.applyType(doc, paramElement, param.getType());
            opElement.appendChild(paramElement);
        }

        parentClass.appendChild(opElement);
    }

    private void exportRelationship(Document doc, Element parent, UmlRelationship rel) {
        if (rel.getType() == RelationshipType.GENERALIZATION) {
            exportGeneralization(doc, parent, rel);
        } else {
            exportAssociation(doc, parent, rel);
        }
    }

    private void exportGeneralization(Document doc, Element rootModel, UmlRelationship rel) {
        // [PENDIENTE VALIDACIÓN EA REAL]
        // Generalization is usually nested under the source class
        // Since we are traversing relationships at the model level, we must find the source class element
        // In a proper implementation we might iterate relationships while exporting the class.
        // For now, we append it to the model level or a placeholder if we don't do second-pass.
        // Actually, let's append it to the source class. We can use DOM to find it.
        String sourceId = idMapper.toXmiId(rel.getSourceClassId());
        String targetId = idMapper.toXmiId(rel.getTargetClassId());

        Element generalization = doc.createElement("generalization");
        generalization.setAttribute("xmi:type", "uml:Generalization");
        generalization.setAttribute("xmi:id", idMapper.toXmiId(rel.getId()));
        generalization.setAttribute("general", targetId);

        // Find source class and append
        var elements = rootModel.getElementsByTagName("packagedElement");
        for (int i = 0; i < elements.getLength(); i++) {
            Element el = (Element) elements.item(i);
            if (sourceId.equals(el.getAttribute("xmi:id"))) {
                el.appendChild(generalization);
                break;
            }
        }
    }

    private void exportAssociation(Document doc, Element parent, UmlRelationship rel) {
        // [PENDIENTE VALIDACIÓN EA REAL]
        Element association = doc.createElement("packagedElement");
        association.setAttribute("xmi:type", "uml:Association");
        association.setAttribute("xmi:id", idMapper.toXmiId(rel.getId()));

        // Source End
        Element memberEnd1 = doc.createElement("memberEnd");
        memberEnd1.setAttribute("xmi:idref", idMapper.toXmiId(rel.getId()) + "_source");
        association.appendChild(memberEnd1);

        // Target End
        Element memberEnd2 = doc.createElement("memberEnd");
        memberEnd2.setAttribute("xmi:idref", idMapper.toXmiId(rel.getId()) + "_target");
        association.appendChild(memberEnd2);

        // Source ownedEnd
        Element sourceEnd = doc.createElement("ownedEnd");
        sourceEnd.setAttribute("xmi:type", "uml:Property");
        sourceEnd.setAttribute("xmi:id", idMapper.toXmiId(rel.getId()) + "_source");
        sourceEnd.setAttribute("type", idMapper.toXmiId(rel.getSourceClassId()));
        sourceEnd.setAttribute("association", idMapper.toXmiId(rel.getId()));
        applyMultiplicity(doc, sourceEnd, rel.getSourceMultiplicity());
        association.appendChild(sourceEnd);

        // Target ownedEnd
        Element targetEnd = doc.createElement("ownedEnd");
        targetEnd.setAttribute("xmi:type", "uml:Property");
        targetEnd.setAttribute("xmi:id", idMapper.toXmiId(rel.getId()) + "_target");
        targetEnd.setAttribute("type", idMapper.toXmiId(rel.getTargetClassId()));
        targetEnd.setAttribute("association", idMapper.toXmiId(rel.getId()));
        
        if (rel.getType() == RelationshipType.AGGREGATION) {
            targetEnd.setAttribute("aggregation", "shared");
        } else if (rel.getType() == RelationshipType.COMPOSITION) {
            targetEnd.setAttribute("aggregation", "composite");
        }
        
        applyMultiplicity(doc, targetEnd, rel.getTargetMultiplicity());
        association.appendChild(targetEnd);

        parent.appendChild(association);
    }

    private void applyMultiplicity(Document doc, Element endElement, String multiplicity) {
        MultiplicityParser.MultiplicityBounds bounds = multiplicityParser.parse(multiplicity);
        
        Element lowerValue = doc.createElement("lowerValue");
        lowerValue.setAttribute("xmi:type", "uml:LiteralInteger");
        lowerValue.setAttribute("value", bounds.lower.equals("*") ? "0" : bounds.lower); // fallback if parsing *
        endElement.appendChild(lowerValue);

        Element upperValue = doc.createElement("upperValue");
        upperValue.setAttribute("xmi:type", "uml:LiteralUnlimitedNatural");
        upperValue.setAttribute("value", bounds.upper);
        endElement.appendChild(upperValue);
    }

    private String mapVisibility(Visibility visibility) {
        if (visibility == null) return "public";
        return visibility.name().toLowerCase();
    }
}
