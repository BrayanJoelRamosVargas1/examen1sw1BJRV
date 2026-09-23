package com.umlcase.infrastructure.web;

import com.umlcase.domain.model.UmlModel;
import com.umlcase.domain.port.UmlModelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.HashSet;
import java.util.Set;
import com.umlcase.domain.model.UmlClass;
import com.umlcase.domain.model.Visibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class GenerateBackendApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UmlModelRepository modelRepository;

    @Test
    void generateBackend_returnsZipFile() throws Exception {
        UUID projectId = UUID.randomUUID();
        UmlModel model = new UmlModel(UUID.randomUUID(), projectId, 0L);
        
        UmlClass cliente = UmlClass.create("Cliente");
        model.addClass(cliente);
        model.addAttribute(cliente.getId(), "nombre", "String", Visibility.PUBLIC);

        UmlClass pedido = UmlClass.create("Pedido");
        model.addClass(pedido);
        model.addAttribute(pedido.getId(), "total", "Decimal", Visibility.PUBLIC);
        
        modelRepository.save(model);

        MvcResult result = mockMvc.perform(get("/api/projects/" + projectId + "/generate/backend"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/zip"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"generated-backend.zip\""))
            .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();
        assertThat(zipBytes).isNotEmpty();

        // Verify ZIP contents
        Set<String> entryNames = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }

        assertThat(entryNames).contains(
            "generated-backend/pom.xml",
            "generated-backend/src/main/java/com/generated/app/GeneratedApplication.java",
            "generated-backend/src/main/java/com/generated/app/entity/Cliente.java",
            "generated-backend/src/main/java/com/generated/app/entity/Pedido.java",
            "generated-backend/src/main/java/com/generated/app/repository/ClienteRepository.java",
            "generated-backend/src/main/java/com/generated/app/service/ClienteService.java",
            "generated-backend/src/main/java/com/generated/app/controller/ClienteController.java",
            "generated-backend/src/main/java/com/generated/app/dto/ClienteRequest.java",
            "generated-backend/src/main/resources/application.yml",
            "generated-backend/src/main/resources/db/migration/V1__initial_schema.sql",
            "generated-backend/mvnw",
            "generated-backend/src/test/java/com/generated/app/GeneratedApplicationTests.java"
        );
    }
}
