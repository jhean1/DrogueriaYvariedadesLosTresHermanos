package com.example.DrogueriaLosTresHermanos.modelo;

/**
 * Modelo: PlanSuscripcion
 * Representa un plan de suscripción disponible en la droguería.
 * Preparado para persistencia en base de datos (RF018 - Gestión de suscripciones).
 */
public class PlanSuscripcion {

    private Long id;
    private String nombre;          // "Básico", "Estándar", "Regular", "Premium"
    private String descripcion;
    private int precioMensual;      // en pesos colombianos (COP)
    private int descuentoPorcentaje; // 0–20
    private boolean domicilioPrioritario;
    private boolean ofertasAnticipadas;
    private boolean soporteWhatsApp;
    private boolean accesoExclusivo;

    // ── Constructor vacío (requerido por frameworks) ──
    public PlanSuscripcion() {}

    // ── Constructor completo ──
    public PlanSuscripcion(Long id, String nombre, String descripcion,
                           int precioMensual, int descuentoPorcentaje,
                           boolean domicilioPrioritario, boolean ofertasAnticipadas,
                           boolean soporteWhatsApp, boolean accesoExclusivo) {
        this.id                   = id;
        this.nombre               = nombre;
        this.descripcion          = descripcion;
        this.precioMensual        = precioMensual;
        this.descuentoPorcentaje  = descuentoPorcentaje;
        this.domicilioPrioritario = domicilioPrioritario;
        this.ofertasAnticipadas   = ofertasAnticipadas;
        this.soporteWhatsApp      = soporteWhatsApp;
        this.accesoExclusivo      = accesoExclusivo;
    }

    // ── Getters y Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getPrecioMensual() { return precioMensual; }
    public void setPrecioMensual(int precioMensual) { this.precioMensual = precioMensual; }

    public int getDescuentoPorcentaje() { return descuentoPorcentaje; }
    public void setDescuentoPorcentaje(int descuentoPorcentaje) {
        this.descuentoPorcentaje = descuentoPorcentaje;
    }

    public boolean isDomicilioPrioritario() { return domicilioPrioritario; }
    public void setDomicilioPrioritario(boolean domicilioPrioritario) {
        this.domicilioPrioritario = domicilioPrioritario;
    }

    public boolean isOfertasAnticipadas() { return ofertasAnticipadas; }
    public void setOfertasAnticipadas(boolean ofertasAnticipadas) {
        this.ofertasAnticipadas = ofertasAnticipadas;
    }

    public boolean isSoporteWhatsApp() { return soporteWhatsApp; }
    public void setSoporteWhatsApp(boolean soporteWhatsApp) {
        this.soporteWhatsApp = soporteWhatsApp;
    }

    public boolean isAccesoExclusivo() { return accesoExclusivo; }
    public void setAccesoExclusivo(boolean accesoExclusivo) {
        this.accesoExclusivo = accesoExclusivo;
    }

    @Override
    public String toString() {
        return "PlanSuscripcion{id=" + id + ", nombre='" + nombre + "', precio=" + precioMensual
             + ", descuento=" + descuentoPorcentaje + "%}";
    }
}
