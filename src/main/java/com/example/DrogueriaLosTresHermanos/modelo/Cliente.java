package com.example.DrogueriaLosTresHermanos.modelo;

/**
 * Modelo: Cliente
 * Representa un cliente registrado en la droguería.
 * Preparado para persistencia en base de datos (RF001 - Registro de clientes).
 */
public class Cliente {

    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private String direccion;
    private String rol;             // "cliente" | "admin"
    private PlanSuscripcion plan;   // null si no tiene suscripción activa

    // ── Constructor vacío ──
    public Cliente() {}

    // ── Constructor básico (registro) ──
    public Cliente(Long id, String nombre, String correo, String telefono, String rol) {
        this.id       = id;
        this.nombre   = nombre;
        this.correo   = correo;
        this.telefono = telefono;
        this.rol      = rol;
    }

    // ── Getters y Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public PlanSuscripcion getPlan() { return plan; }
    public void setPlan(PlanSuscripcion plan) { this.plan = plan; }

    /** Devuelve true si el cliente tiene una suscripción activa con descuento */
    public boolean tieneSuscripcionActiva() {
        return plan != null && plan.getDescuentoPorcentaje() > 0;
    }

    @Override
    public String toString() {
        return "Cliente{id=" + id + ", nombre='" + nombre + "', correo='" + correo + "'}";
    }
}
