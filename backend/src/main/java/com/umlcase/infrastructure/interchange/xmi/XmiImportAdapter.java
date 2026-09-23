package com.umlcase.infrastructure.interchange.xmi;

import com.umlcase.application.port.out.UmlInterchangeImporter;
import com.umlcase.domain.model.*;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class XmiImportAdapter implements UmlInterchangeImporter {

    private final XmiIdMapper idMapper = new XmiIdMapper();
    private final MultiplicityParser multiplicityParser = new MultiplicityParser();
    // We reverse map types by just taking the name for now, or using a known mapper logic
    // We will parse type from <type href="...#Type"> or simple names

    @Override
    public UmlModel importModel(byte[] content) {
        if (content.length > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 5MB");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Seguridad XML OBLIGATORIA
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new ByteArrayInputStream(content)));

            // Transitorio, no usaremos UUID ni version real aquí porque se reemplazarán luego
            UmlModel model = new UmlModel(UUID.randomUUID(), UUID.randomUUID(), 0L);

            NodeList elements = doc.getElementsByTagName("packagedElement");

            // 1. First pass: Create all classes
            for (int i = 0; i < elements.getLength(); i++) {
                Element el = (Element) elements.item(i);
                if ("uml:Class".equals(el.getAttribute("xmi:type"))) {
                    parseClass(el, model);
                }
            }

            // 2. Second pass: Create relationships (they need source/target classes to exist)
            // Some associations are packagedElements, some generalizations are nested inside classes
            for (int i = 0; i < elements.getLength(); i++) {
                Element el = (Element) elements.item(i);
                if ("uml:Class".equals(el.getAttribute("xmi:type"))) {
                    parseGeneralizations(el, model);
                } else if ("uml:Association".equals(el.getAttribute("xmi:type"))) {
                    parseAssociation(el, model);
                }
            }

            return model;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing XMI: " + e.getMessage(), e);
        }
    }

    private void parseClass(Element classEl, UmlModel model) {
        String xmiId = classEl.getAttribute("xmi:id");
        String name = classEl.getAttribute("name");
        UUID classId = idMapper.toUuid(xmiId);

        UmlClass cls = new UmlClass(classId, name);

        // Attributes
        NodeList attrs = classEl.getElementsByTagName("ownedAttribute");
        for (int i = 0; i < attrs.getLength(); i++) {
            Element attrEl = (Element) attrs.item(i);
            // Ignore if it's an association end (usually they have an "association" attribute)
            if (attrEl.hasAttribute("association")) continue;
            
            String attrIdStr = attrEl.getAttribute("xmi:id");
            String attrName = attrEl.getAttribute("name");
            String visibilityStr = attrEl.getAttribute("visibility");
            String type = parseType(attrEl);
            
            UmlAttribute attr = new UmlAttribute(
                    idMapper.toUuid(attrIdStr),
                    attrName != null && !attrName.isEmpty() ? attrName : "unnamed",
                    type,
                    parseVisibility(visibilityStr),
                    i
            );
            cls.addAttribute(attr);
        }

        // Operations
        NodeList ops = classEl.getElementsByTagName("ownedOperation");
        for (int i = 0; i < ops.getLength(); i++) {
            Element opEl = (Element) ops.item(i);
            String opIdStr = opEl.getAttribute("xmi:id");
            String opName = opEl.getAttribute("name");
            String visibilityStr = opEl.getAttribute("visibility");

            List<UmlParameter> parameters = new ArrayList<>();
            String returnType = "void";

            UmlOperation op = new UmlOperation(
                    idMapper.toUuid(opIdStr),
                    opName != null && !opName.isEmpty() ? opName : "unnamedOp",
                    returnType,
                    parseVisibility(visibilityStr),
                    i
            );
            
            NodeList params = opEl.getElementsByTagName("ownedParameter");
            for (int j = 0; j < params.getLength(); j++) {
                Element paramEl = (Element) params.item(j);
                String direction = paramEl.getAttribute("direction");
                String paramType = parseType(paramEl);
                
                if ("return".equals(direction)) {
                    op.changeReturnType(paramType);
                } else {
                    String paramName = paramEl.getAttribute("name");
                    op.addParameter(new UmlParameter(
                            idMapper.toUuid(paramEl.getAttribute("xmi:id")),
                            paramName != null && !paramName.isEmpty() ? paramName : "param" + j,
                            paramType,
                            j
                    ));
                }
            }

            cls.addOperation(op);
        }

        model.addClass(cls);
    }

    private void parseGeneralizations(Element classEl, UmlModel model) {
        String sourceXmiId = classEl.getAttribute("xmi:id");
        NodeList genEls = classEl.getElementsByTagName("generalization");
        for (int i = 0; i < genEls.getLength(); i++) {
            Element genEl = (Element) genEls.item(i);
            String targetXmiId = genEl.getAttribute("general");
            String genXmiId = genEl.getAttribute("xmi:id");
            
            if (targetXmiId != null && !targetXmiId.isEmpty()) {
                model.addRelationship(new UmlRelationship(
                        idMapper.toUuid(genXmiId),
                        RelationshipType.GENERALIZATION,
                        idMapper.toUuid(sourceXmiId),
                        idMapper.toUuid(targetXmiId),
                        "1", "1"
                ));
            }
        }
    }

    private void parseAssociation(Element assocEl, UmlModel model) {
        String assocXmiId = assocEl.getAttribute("xmi:id");
        
        // Find ownedEnds
        NodeList ends = assocEl.getElementsByTagName("ownedEnd");
        if (ends.getLength() >= 2) {
            Element sourceEnd = (Element) ends.item(0);
            Element targetEnd = (Element) ends.item(1);

            String sourceClassXmiId = sourceEnd.getAttribute("type");
            String targetClassXmiId = targetEnd.getAttribute("type");

            if (sourceClassXmiId.isEmpty() || targetClassXmiId.isEmpty()) return;

            String targetAggregation = targetEnd.getAttribute("aggregation");
            RelationshipType relType = RelationshipType.ASSOCIATION;
            if ("shared".equals(targetAggregation)) {
                relType = RelationshipType.AGGREGATION;
            } else if ("composite".equals(targetAggregation)) {
                relType = RelationshipType.COMPOSITION;
            }

            String sourceMultiplicity = parseMultiplicity(sourceEnd);
            String targetMultiplicity = parseMultiplicity(targetEnd);

            model.addRelationship(new UmlRelationship(
                    idMapper.toUuid(assocXmiId),
                    relType,
                    idMapper.toUuid(sourceClassXmiId),
                    idMapper.toUuid(targetClassXmiId),
                    sourceMultiplicity,
                    targetMultiplicity
            ));
        }
    }

    private String parseType(Element element) {
        // Try nested <type>
        NodeList types = element.getElementsByTagName("type");
        if (types.getLength() > 0) {
            Element typeEl = (Element) types.item(0);
            String href = typeEl.getAttribute("href");
            if (href != null && href.contains("#")) {
                return href.substring(href.lastIndexOf("#") + 1);
            }
        }
        return "String"; // Default fallback
    }

    private String parseMultiplicity(Element endElement) {
        String lower = "1";
        String upper = "1";

        NodeList lowerEls = endElement.getElementsByTagName("lowerValue");
        if (lowerEls.getLength() > 0) {
            lower = ((Element) lowerEls.item(0)).getAttribute("value");
            if (lower.isEmpty()) lower = "0"; // default if missing attribute
        }

        NodeList upperEls = endElement.getElementsByTagName("upperValue");
        if (upperEls.getLength() > 0) {
            upper = ((Element) upperEls.item(0)).getAttribute("value");
            if (upper.isEmpty()) upper = "1";
        }

        if (lower.equals(upper)) return lower;
        if (lower.equals("0") && upper.equals("*")) return "*";
        return lower + ".." + upper;
    }

    private Visibility parseVisibility(String visibility) {
        if ("private".equalsIgnoreCase(visibility)) return Visibility.PRIVATE;
        if ("protected".equalsIgnoreCase(visibility)) return Visibility.PROTECTED;
        if ("package".equalsIgnoreCase(visibility)) return Visibility.PACKAGE;
        return Visibility.PUBLIC; // Default
    }
}
