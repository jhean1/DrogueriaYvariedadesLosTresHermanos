package com.example.DrogueriaLosTresHermanos.modelo;

/**
 * Modelo: Producto
 * Representa un artículo del catálogo (medicamento o variedad).
 * Preparado para persistencia en base de datos (RF009 - Catálogo de productos).
 */
public class Producto {

    private Long id;
    private String nombre;
    private String descripcion;
    private String categoria;       // "analgesicos", "antibioticos", "perfumes", etc.
    private String tipo;            // "medicamento" | "variedad"
    private double precio;
    private String imagen;
    private boolean disponible;

    // ── Constructor vacío ──
    public Producto() {}

    // ── Constructor completo ──
    public Producto(Long id, String nombre, String descripcion,
                    String categoria, String tipo, double precio,
                    String imagen, boolean disponible) {
        this.id          = id;
        this.nombre      = nombre;
        this.descripcion = descripcion;
        this.categoria   = categoria;
        this.tipo        = tipo;
        this.precio      = precio;
        this.imagen      = imagen;
        this.disponible  = disponible;
    }

    // ── Getters y Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    /**
     * Calcula el precio con descuento según el plan del cliente.
     * @param porcentaje porcentaje de descuento (0–20)
     */
    public double getPrecioConDescuento(int porcentaje) {
        if (porcentaje <= 0) return precio;
        return Math.round(precio * (1 - porcentaje / 100.0));
    }

    @Override
    public String toString() {
        return "Producto{id=" + id + ", nombre='" + nombre + "', precio=" + precio + "}";
    }
}
