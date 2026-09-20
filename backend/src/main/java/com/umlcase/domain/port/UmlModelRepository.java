package com.umlcase.domain.port;

import com.umlcase.domain.model.UmlModel;

import java.util.Optional;
import java.util.UUID;

/**
 * PUERTO DE DOMINIO — Repositorio del modelo UML.
 *
 * [ADR-001] Arquitectura de puertos y adaptadores.
 *
 * Esta interfaz define el contrato que el dominio y la aplicación
 * necesitan para persistir y recuperar el UmlModel.
 *
 * El dominio depende de ESTA INTERFAZ (puerto), no de JPA.
 * JPA implementa esta interfaz en la capa de infraestructura (adaptador).
 *
 * Pregunta oral esperada:
 *  - ¿Qué es un puerto? → Una interfaz que desacopla el dominio de la infraestructura.
 *  - ¿Por qué no usar JpaRepository directamente? → Porque el dominio no debe
 *    conocer JPA. Si cambiamos a MongoDB, solo cambia el adaptador, no el dominio.
 */
public interface UmlModelRepository {

    /**
     * Persiste o actualiza un UmlModel.
     * Si ya existe un modelo con el mismo id, lo reemplaza.
     *
     * @param model el modelo a persistir (no debe ser null)
     * @return el modelo persistido (puede incluir campos generados por infraestructura)
     */
    UmlModel save(UmlModel model);

    /**
     * Recupera un UmlModel por su id.
     *
     * @param modelId el id del modelo
     * @return Optional con el modelo, o empty si no existe
     */
    Optional<UmlModel> findById(UUID modelId);

    /**
     * Recupera el modelo UML de un proyecto específico.
     * Cada proyecto tiene exactamente un UmlModel.
     *
     * @param projectId el id del proyecto
     * @return Optional con el modelo, o empty si el proyecto no tiene modelo todavía
     */
    Optional<UmlModel> findByProjectId(UUID projectId);

    /**
     * Elimina un UmlModel y todo su contenido.
     *
     * @param modelId el id del modelo a eliminar
     */
    void deleteById(UUID modelId);

    /**
     * Verifica si existe un modelo para el proyecto dado.
     *
     * @param projectId el id del proyecto
     * @return true si el proyecto ya tiene un modelo
     */
    boolean existsByProjectId(UUID projectId);
}
