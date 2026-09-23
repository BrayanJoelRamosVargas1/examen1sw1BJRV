package com.umlcase.infrastructure.interchange.xmi;

import com.umlcase.domain.model.UmlModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Xmi21ImporterTest — Parser y Seguridad XML")
class Xmi21ImporterTest {

    private final XmiImportAdapter importer = new XmiImportAdapter();

    @Test
    @DisplayName("Importar modelo válido sin layout parsea correctamente")
    void testValidImport() {
        String xmi = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xmi:XMI xmlns:xmi="http://schema.omg.org/spec/XMI/2.1" xmlns:uml="http://schema.omg.org/spec/UML/2.1" xmi:version="2.1">
                  <uml:Model name="UmlModelExport" xmi:id="EAID_UmlModelExport" xmi:type="uml:Model">
                    <packagedElement name="ClassA" xmi:id="EAID_1" xmi:type="uml:Class">
                      <ownedAttribute name="attr1" visibility="private" xmi:id="EAID_attr1" xmi:type="uml:Property">
                        <type href="http://schema.omg.org/spec/UML/2.1/uml.xml#String" xmi:type="uml:PrimitiveType"/>
                      </ownedAttribute>
                      <ownedOperation name="op1" visibility="public" xmi:id="EAID_op1" xmi:type="uml:Operation">
                        <ownedParameter direction="return" xmi:id="EAID_op1_return" xmi:type="uml:Parameter">
                          <type href="http://schema.omg.org/spec/UML/2.1/uml.xml#Integer" xmi:type="uml:PrimitiveType"/>
                        </ownedParameter>
                        <ownedParameter direction="in" name="param1" xmi:id="EAID_param1" xmi:type="uml:Parameter">
                          <type href="http://schema.omg.org/spec/UML/2.1/uml.xml#String" xmi:type="uml:PrimitiveType"/>
                        </ownedParameter>
                      </ownedOperation>
                    </packagedElement>
                    <packagedElement name="ClassB" xmi:id="EAID_2" xmi:type="uml:Class">
                    </packagedElement>
                    <packagedElement xmi:type="uml:Association" xmi:id="EAID_rel1">
                      <memberEnd xmi:idref="EAID_rel1_source"/>
                      <memberEnd xmi:idref="EAID_rel1_target"/>
                      <ownedEnd xmi:type="uml:Property" xmi:id="EAID_rel1_source" type="EAID_1" association="EAID_rel1">
                        <lowerValue xmi:type="uml:LiteralInteger" value="1"/>
                        <upperValue xmi:type="uml:LiteralUnlimitedNatural" value="1"/>
                      </ownedEnd>
                      <ownedEnd xmi:type="uml:Property" xmi:id="EAID_rel1_target" type="EAID_2" association="EAID_rel1" aggregation="shared">
                        <lowerValue xmi:type="uml:LiteralInteger" value="0"/>
                        <upperValue xmi:type="uml:LiteralUnlimitedNatural" value="*"/>
                      </ownedEnd>
                    </packagedElement>
                  </uml:Model>
                </xmi:XMI>
                """;

        UmlModel model = importer.importModel(xmi.getBytes(StandardCharsets.UTF_8));
        
        assertThat(model.getClasses()).hasSize(2);
        assertThat(model.getRelationships()).hasSize(1);

        var classA = model.findClassByName("ClassA").orElseThrow();
        assertThat(classA.getAttributes()).hasSize(1);
        assertThat(classA.getAttributes().get(0).getName()).isEqualTo("attr1");
        assertThat(classA.getAttributes().get(0).getType()).isEqualTo("String");

        assertThat(classA.getOperations()).hasSize(1);
        assertThat(classA.getOperations().get(0).getName()).isEqualTo("op1");
        assertThat(classA.getOperations().get(0).getReturnType()).isEqualTo("Integer");
        assertThat(classA.getOperations().get(0).getParameters()).hasSize(1);
        
        var rel = model.getRelationships().get(0);
        assertThat(rel.getType()).isEqualTo(com.umlcase.domain.model.RelationshipType.AGGREGATION);
        assertThat(rel.getSourceClassId()).isEqualTo(classA.getId());
        assertThat(rel.getSourceMultiplicity()).isEqualTo("1");
        assertThat(rel.getTargetMultiplicity()).isEqualTo("*");
    }

    @Test
    @DisplayName("Seguridad: Rechaza entidades externas (XXE)")
    void testRejectXXE() {
        String xxe = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <xmi:XMI xmlns:xmi="http://schema.omg.org/spec/XMI/2.1">
                  <foo>&xxe;</foo>
                </xmi:XMI>
                """;
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
                importer.importModel(xxe.getBytes(StandardCharsets.UTF_8))
        );
        
        assertThat(exception.getMessage()).contains("DOCTYPE");
    }

    @Test
    @DisplayName("Seguridad: Rechaza DOCTYPE")
    void testRejectDoctype() {
        String doctype = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE xmi:XMI []>
                <xmi:XMI xmlns:xmi="http://schema.omg.org/spec/XMI/2.1">
                </xmi:XMI>
                """;
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
                importer.importModel(doctype.getBytes(StandardCharsets.UTF_8))
        );
        
        assertThat(exception.getMessage()).contains("DOCTYPE");
    }

    @Test
    @DisplayName("Seguridad: Límite de tamaño 5MB")
    void testRejectOversized() {
        byte[] oversized = new byte[6 * 1024 * 1024]; // 6MB
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
                importer.importModel(oversized)
        );
        
        assertThat(exception.getMessage()).contains("5MB");
    }

    @Test
    @DisplayName("Fallo: XML inválido")
    void testInvalidXml() {
        String invalid = "<xmi:XMI><unclosed>";
        assertThrows(RuntimeException.class, () -> 
                importer.importModel(invalid.getBytes(StandardCharsets.UTF_8))
        );
    }
}
