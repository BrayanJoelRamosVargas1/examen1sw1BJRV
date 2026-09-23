package com.umlcase.infrastructure.interchange.xmi;

import com.umlcase.domain.model.*;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Xmi21ExporterTest {

    private final XmiExportAdapter exporter = new XmiExportAdapter();

    private Document parseXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlBytes));
    }

    @Test
    void testEmptyModelExport() throws Exception {
        UmlModel model = new UmlModel(UUID.randomUUID(), UUID.randomUUID(), 1L);
        byte[] result = exporter.export(model);
        
        assertNotNull(result);
        assertTrue(result.length > 0);
        String xmlString = new String(result, "UTF-8");
        assertTrue(xmlString.contains("xmi:XMI"));
        assertTrue(xmlString.contains("uml:Model"));

        Document doc = parseXml(result);
        assertEquals("XMI", doc.getDocumentElement().getLocalName());
    }

    @Test
    void testClassExportWithDeterminism() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlClass cls = new UmlClass(UUID.randomUUID(), "TestClass");
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 1L);
        model.addClass(cls);

        byte[] result1 = exporter.export(model);
        byte[] result2 = exporter.export(model);

        assertArrayEquals(result1, result2, "Export should be deterministic");

        Document doc = parseXml(result1);
        NodeList classes = doc.getElementsByTagName("packagedElement");
        assertEquals(1, classes.getLength());
        assertEquals("TestClass", classes.item(0).getAttributes().getNamedItem("name").getNodeValue());
        assertEquals("uml:Class", classes.item(0).getAttributes().getNamedItem("xmi:type").getNodeValue());
    }

    @Test
    void testAttributeAndPrimitiveTypeExport() throws Exception {
        UmlAttribute attr = new UmlAttribute(UUID.randomUUID(), "active", "Boolean", Visibility.PRIVATE, 0);
        UmlClass cls = new UmlClass(UUID.randomUUID(), "TestClass");
        cls.addAttribute(attr);
        UmlModel model = new UmlModel(UUID.randomUUID(), UUID.randomUUID(), 1L);
        model.addClass(cls);

        Document doc = parseXml(exporter.export(model));
        NodeList attrs = doc.getElementsByTagName("ownedAttribute");
        assertEquals(1, attrs.getLength());
        assertEquals("active", attrs.item(0).getAttributes().getNamedItem("name").getNodeValue());
        assertEquals("private", attrs.item(0).getAttributes().getNamedItem("visibility").getNodeValue());

        NodeList types = doc.getElementsByTagName("type");
        assertEquals(1, types.getLength());
        assertEquals("http://schema.omg.org/spec/UML/2.1/uml.xml#Boolean", types.item(0).getAttributes().getNamedItem("href").getNodeValue());
    }

    @Test
    void testOperationExport() throws Exception {
        UmlOperation op = new UmlOperation(UUID.randomUUID(), "calculate", "Decimal", Visibility.PUBLIC, 0);
        UmlParameter param = new UmlParameter(UUID.randomUUID(), "amount", "Integer", 0);
        op.addParameter(param);
        UmlClass cls = new UmlClass(UUID.randomUUID(), "TestClass");
        cls.addOperation(op);
        UmlModel model = new UmlModel(UUID.randomUUID(), UUID.randomUUID(), 1L);
        model.addClass(cls);

        Document doc = parseXml(exporter.export(model));
        NodeList ops = doc.getElementsByTagName("ownedOperation");
        assertEquals(1, ops.getLength());
        assertEquals("calculate", ops.item(0).getAttributes().getNamedItem("name").getNodeValue());
        assertEquals("public", ops.item(0).getAttributes().getNamedItem("visibility").getNodeValue());

        NodeList params = doc.getElementsByTagName("ownedParameter");
        assertEquals(2, params.getLength()); // one for return, one for param
    }

    @Test
    void testAssociationExport() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UmlClass source = new UmlClass(sourceId, "Source");
        UmlClass target = new UmlClass(targetId, "Target");
        UmlRelationship rel = new UmlRelationship(UUID.randomUUID(), RelationshipType.ASSOCIATION, sourceId, targetId, "1", "0..*");
        UmlModel model = new UmlModel(UUID.randomUUID(), UUID.randomUUID(), 1L);
        model.addClass(source);
        model.addClass(target);
        model.addRelationship(rel);

        Document doc = parseXml(exporter.export(model));
        NodeList associations = doc.getElementsByTagName("packagedElement");
        // Source, Target, Association -> 3 elements
        assertEquals(3, associations.getLength());

        NodeList ownedEnds = doc.getElementsByTagName("ownedEnd");
        assertEquals(2, ownedEnds.getLength());
        
        NodeList lowerValues = doc.getElementsByTagName("lowerValue");
        assertEquals(2, lowerValues.getLength());
        NodeList upperValues = doc.getElementsByTagName("upperValue");
        assertEquals(2, upperValues.getLength());
    }

    @Test
    void testGeneralizationExport() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UmlClass parent = new UmlClass(parentId, "Parent");
        UmlClass child = new UmlClass(childId, "Child");
        UmlRelationship rel = new UmlRelationship(UUID.randomUUID(), RelationshipType.GENERALIZATION, childId, parentId, "", "");
        UmlModel model = new UmlModel(UUID.randomUUID(), UUID.randomUUID(), 1L);
        model.addClass(parent);
        model.addClass(child);
        model.addRelationship(rel);

        Document doc = parseXml(exporter.export(model));
        NodeList generalizations = doc.getElementsByTagName("generalization");
        assertEquals(1, generalizations.getLength());
        
        String generalRef = generalizations.item(0).getAttributes().getNamedItem("general").getNodeValue();
        assertEquals("EAID_" + parentId.toString(), generalRef);
    }
}
