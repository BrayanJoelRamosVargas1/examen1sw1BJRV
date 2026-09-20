package com.umlcase.diagram.model;

import java.util.Objects;
import java.util.UUID;

/**
 * REPRESENTACIÓN VISUAL — Posición y tamaño de una clase en el canvas.
 *
 * [ADR-002] Separación semántica/visual.
 * Esta clase representa DÓNDE está un elemento UML en pantalla,
 * NO qué significa ese elemento.
 *
 * Mover una clase en el canvas actualiza UmlNodeView.
 * Renombrar una clase actualiza UmlClass.
 * Son operaciones completamente independientes.
 *
 * Pregunta oral esperada:
 *  - ¿Por qué existe UmlNodeView separado de UmlClass?
 *    → Porque exportar a XMI o generar Spring Boot no necesita coordenadas de pantalla.
 *      Cambiar de GoJS a otra biblioteca no afecta el modelo semántico.
 */
public final class UmlNodeView {

    private final UUID id;
    private final UUID elementId;   // Referencia al id de UmlClass (no contiene la clase)
    private double x;
    private double y;
    private double width;
    private double height;

    public UmlNodeView(UUID id, UUID elementId, double x, double y,
                       double width, double height) {
        this.id = Objects.requireNonNull(id);
        this.elementId = Objects.requireNonNull(elementId, "elementId no puede ser null");
        this.x = x;
        this.y = y;
        this.width = width > 0 ? width : 150.0;
        this.height = height > 0 ? height : 100.0;
    }

    public static UmlNodeView create(UUID elementId, double x, double y) {
        return new UmlNodeView(UUID.randomUUID(), elementId, x, y, 150.0, 100.0);
    }

    // ─── Mutaciones ───────────────────────────────────────────────────────────

    /** Mueve el nodo a nuevas coordenadas. */
    public void moveTo(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    /** Redimensiona el nodo. */
    public void resize(double newWidth, double newHeight) {
        this.width = newWidth > 0 ? newWidth : 150.0;
        this.height = newHeight > 0 ? newHeight : 100.0;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()        { return id; }
    public UUID getElementId() { return elementId; }
    public double getX()       { return x; }
    public double getY()       { return y; }
    public double getWidth()   { return width; }
    public double getHeight()  { return height; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UmlNodeView other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "UmlNodeView{elementId=" + elementId
               + ", x=" + x + ", y=" + y + "}";
    }
}
