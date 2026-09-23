package com.umlcase.application.mapper.java;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaNamingStrategyTest {

    private final JavaNamingStrategy naming = new JavaNamingStrategy();

    @Test
    void toClassName_validName_capitalizes() {
        assertThat(naming.toClassName("cliente")).isEqualTo("Cliente");
        assertThat(naming.toClassName("Cliente_VIP")).isEqualTo("Cliente_VIP");
    }

    @Test
    void toClassName_reservedWord_appendsEntity() {
        assertThat(naming.toClassName("class")).isEqualTo("ClassEntity");
        assertThat(naming.toClassName("public")).isEqualTo("PublicEntity");
    }

    @Test
    void toClassName_invalidCharacters_areRemoved() {
        assertThat(naming.toClassName("Mi-Clase Especial!")).isEqualTo("MiClaseEspecial");
    }

    @Test
    void toClassName_startsWithDigit_prependsUnderscore() {
        assertThat(naming.toClassName("1erCliente")).isEqualTo("_1erCliente");
    }

    @Test
    void toFieldName_validName_camelCases() {
        assertThat(naming.toFieldName("NombreCompleto")).isEqualTo("nombreCompleto");
        assertThat(naming.toFieldName("activo")).isEqualTo("activo");
    }

    @Test
    void toFieldName_reservedWord_prependsUnderscore() {
        assertThat(naming.toFieldName("class")).isEqualTo("_class");
        assertThat(naming.toFieldName("public")).isEqualTo("_public");
    }

    @Test
    void toFieldName_separatorsCreateCamelCase_andInvalidCharactersAreRemoved() {
        assertThat(naming.toFieldName("mi-campo.nuevo!")).isEqualTo("miCampoNuevo");
    }

    @Test
    void emptyOrNull_throwsException() {
        assertThatThrownBy(() -> naming.toClassName(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> naming.toFieldName("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> naming.toClassName(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
