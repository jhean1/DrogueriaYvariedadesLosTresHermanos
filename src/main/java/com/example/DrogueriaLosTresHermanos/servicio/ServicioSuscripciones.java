package com.example.DrogueriaLosTresHermanos.servicio;

import com.example.DrogueriaLosTresHermanos.servicio.BaseDatos.ErrorUsuario;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Planes, solicitudes de pago y suscripciones de los clientes. El descuento siempre sale de la base de datos. */
@Service
public class ServicioSuscripciones {

    public record Plan(int id, String nombre, String descripcion, int precio, int descuento, int dias,
                       boolean domicilioPrioritario, boolean ofertasAnticipadas, boolean soporteWhatsapp, boolean accesoExclusivo) {}

    /** Suscripción que da descuento hoy. Si el cliente la canceló, conserva el descuento hasta que vence. */
    public record Vigente(long id, String plan, int descuento, String inicio, String vence, String estado) {}

    private static final List<String> METODOS_EN_LINEA = List.of("nequi", "daviplata", "pse", "bancolombia");

    private final BaseDatos bd;

    public ServicioSuscripciones(BaseDatos bd) {
        this.bd = bd;
    }

    public List<Plan> planes() {
        return bd.jdbc().query("SELECT id, nombre_plan, descripcion_plan, precio_mensual, descuento_porcentaje, duracion_dias, domicilio_prioritario, "
                + "ofertas_anticipadas, soporte_whatsapp, acceso_exclusivo FROM plan_suscripcion WHERE plan_activo = TRUE ORDER BY precio_mensual",
            (r, n) -> new Plan(r.getInt(1), r.getString(2), r.getString(3), r.getBigDecimal(4).intValue(), r.getInt(5), r.getInt(6),
                r.getBoolean(7), r.getBoolean(8), r.getBoolean(9), r.getBoolean(10)));
    }

    /** Medio de pago en línea que la droguería ya publicó: a dónde enviar el dinero y a nombre de quién. */
    public record MedioPago(String codigo, String nombre, String destino, String titular) {}

    /**
     * Solo se ofrecen los medios cuya cuenta está escrita en la configuración (pago_nequi, pago_daviplata,
     * pago_pse, pago_bancolombia y pago_titular). Así la página nunca muestra un número de cuenta inventado.
     */
    public List<MedioPago> mediosDePago() {
        Map<String, String> config = new java.util.HashMap<>();
        bd.jdbc().query("SELECT clave_configuracion, valor_configuracion FROM configuracion WHERE clave_configuracion LIKE 'pago\\_%'",
            r -> {
                config.put(r.getString(1), r.getString(2) == null ? "" : r.getString(2).strip());
            });
        String titular = config.getOrDefault("pago_titular", "");
        return bd.jdbc().query("SELECT codigo_metodo_pago, nombre_metodo_pago FROM metodo_pago ORDER BY id",
                (r, n) -> new MedioPago(r.getString(1), r.getString(2), config.getOrDefault("pago_" + r.getString(1), ""), titular))
            .stream().filter(m -> METODOS_EN_LINEA.contains(m.codigo()) && !m.destino().isEmpty()).toList();
    }

    public Optional<Vigente> vigente(long cliente) {
        return bd.jdbc().query("SELECT s.id, p.nombre_plan, s.descuento_porcentaje, DATE_FORMAT(s.fecha_inicio, '%Y-%m-%d'), "
                + "DATE_FORMAT(s.fecha_vencimiento, '%Y-%m-%d'), s.estado_suscripcion FROM suscripcion s JOIN plan_suscripcion p ON p.id = s.plan_id "
                + "WHERE s.cliente_id = ? AND s.estado_suscripcion IN ('activa', 'cancelada') AND s.fecha_vencimiento > NOW() "
                + "ORDER BY s.estado_suscripcion = 'activa' DESC, s.fecha_vencimiento DESC LIMIT 1",
            (r, n) -> new Vigente(r.getLong(1), r.getString(2), r.getInt(3), r.getString(4), r.getString(5), r.getString(6)), cliente).stream().findFirst();
    }

    /** Solicitud de compra en línea. El monto lo fija el servidor con el precio del plan. */
    public long solicitar(long cliente, String nombrePlan, String metodo, String referencia, String aprobacion, String titular,
                          String fechaPago, String nota, String comprobante) {
        Plan plan = planPorNombre(nombrePlan);
        if (mediosDePago().stream().noneMatch(m -> m.codigo().equals(metodo))) {
            throw new ErrorUsuario("Elige cómo hiciste el pago.");
        }
        if (referencia == null || referencia.isBlank() || referencia.strip().length() > 60) {
            throw new ErrorUsuario("Escribe el número de referencia de la transacción.");
        }
        if (titular == null || titular.isBlank() || titular.strip().length() > 120) {
            throw new ErrorUsuario("Escribe el nombre del titular de la cuenta que pagó.");
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaPago);
        } catch (RuntimeException e) {
            throw new ErrorUsuario("Escribe la fecha del pago.");
        }
        if (fecha.isAfter(LocalDate.now()) || fecha.isBefore(LocalDate.now().minusDays(30))) {
            throw new ErrorUsuario("La fecha del pago debe estar dentro de los últimos 30 días.");
        }
        Integer pendientes = bd.jdbc().queryForObject("SELECT COUNT(*) FROM solicitud_suscripcion WHERE cliente_id = ? AND estado_solicitud = 'pendiente'",
            Integer.class, cliente);
        if (pendientes != null && pendientes > 0) {
            throw new ErrorUsuario("Ya tienes un pago en revisión. Espera a que la droguería lo confirme.");
        }
        bd.jdbc().update("INSERT INTO solicitud_suscripcion (cliente_id, plan_id, metodo_pago_id, origen_solicitud, monto_pagado, referencia_pago, "
                + "numero_aprobacion, titular_pago, fecha_pago, comprobante_pago, nota_solicitud) "
                + "VALUES (?, ?, (SELECT id FROM metodo_pago WHERE codigo_metodo_pago = ?), 'web', ?, ?, ?, ?, ?, ?, ?)",
            cliente, plan.id(), metodo, plan.precio(), referencia.strip(), vacio(aprobacion, 60), titular.strip(), fecha, comprobante, vacio(nota, 255));
        return bd.jdbc().queryForObject("SELECT MAX(id) FROM solicitud_suscripcion WHERE cliente_id = ?", Long.class, cliente);
    }

    public List<Map<String, Object>> solicitudesDe(long cliente) {
        return bd.jdbc().queryForList("SELECT s.id, p.nombre_plan AS plan, s.monto_pagado AS monto, m.nombre_metodo_pago AS metodo, s.estado_solicitud AS estado, "
            + "s.motivo_rechazo AS motivo, DATE_FORMAT(s.fecha_solicitud, '%Y-%m-%d %H:%i') AS fecha FROM solicitud_suscripcion s "
            + "JOIN plan_suscripcion p ON p.id = s.plan_id JOIN metodo_pago m ON m.id = s.metodo_pago_id WHERE s.cliente_id = ? ORDER BY s.id DESC", cliente);
    }

    public void cancelar(long cliente) {
        int n = bd.jdbc().update("UPDATE suscripcion SET estado_suscripcion = 'cancelada', fecha_cancelacion = NOW() "
            + "WHERE cliente_id = ? AND estado_suscripcion = 'activa'", cliente);
        if (n == 0) {
            throw new ErrorUsuario("No tienes un plan activo para cancelar.");
        }
    }

    // ── Panel ─────────────────────────────────────────────────

    public List<Map<String, Object>> solicitudesParaPanel() {
        return bd.jdbc().queryForList("SELECT s.id, s.cliente_id AS clienteId, c.nombre_cliente AS cliente, c.correo_cliente AS correo, p.nombre_plan AS plan, "
            + "s.monto_pagado AS monto, m.nombre_metodo_pago AS metodo, s.origen_solicitud AS origen, s.referencia_pago AS referencia, "
            + "s.numero_aprobacion AS aprobacion, s.titular_pago AS titular, DATE_FORMAT(s.fecha_pago, '%Y-%m-%d') AS fechaPago, s.nota_solicitud AS nota, "
            + "s.comprobante_pago IS NOT NULL AS tieneComprobante, s.estado_solicitud AS estado, s.motivo_rechazo AS motivo, "
            + "DATE_FORMAT(s.fecha_solicitud, '%Y-%m-%d %H:%i') AS fecha FROM solicitud_suscripcion s JOIN cliente c ON c.id = s.cliente_id "
            + "JOIN plan_suscripcion p ON p.id = s.plan_id JOIN metodo_pago m ON m.id = s.metodo_pago_id ORDER BY s.estado_solicitud = 'pendiente' DESC, s.id DESC");
    }

    /** Archivos de comprobantes de un cliente, para borrarlos cuando elimina su cuenta. */
    public List<String> comprobantesDe(long cliente) {
        return bd.jdbc().queryForList("SELECT comprobante_pago FROM solicitud_suscripcion WHERE cliente_id = ? AND comprobante_pago IS NOT NULL",
            String.class, cliente);
    }

    public Optional<String> comprobante(long solicitud) {
        return bd.jdbc().queryForList("SELECT comprobante_pago FROM solicitud_suscripcion WHERE id = ?", String.class, solicitud).stream()
            .filter(x -> x != null && !x.isBlank()).findFirst();
    }

    /** Aprueba con el procedimiento de la base: verifica el precio y, si renueva, conserva los días que faltaban. */
    public long aprobar(long solicitud, long usuario) {
        try {
            Long id = bd.jdbc().execute("{CALL aprobar_solicitud(?, ?, ?)}", (CallableStatement cs) -> {
                cs.setLong(1, solicitud);
                cs.setLong(2, usuario);
                cs.registerOutParameter(3, Types.BIGINT);
                cs.execute();
                return cs.getLong(3);
            });
            return id == null ? 0 : id;
        } catch (DataAccessException e) {
            String regla = BaseDatos.mensajeDeRegla(e);
            if (regla != null) {
                throw new ErrorUsuario(regla);
            }
            throw e;
        }
    }

    public void rechazar(long solicitud, long usuario, String motivo) {
        String m = motivo == null ? "" : motivo.strip();
        if (m.isEmpty() || m.length() > 255) {
            throw new ErrorUsuario("Escribe por qué se rechaza el pago; el cliente lo verá.");
        }
        int n = bd.jdbc().update("UPDATE solicitud_suscripcion SET estado_solicitud = 'rechazado', motivo_rechazo = ?, revisado_por_id = ?, "
            + "fecha_revision = NOW() WHERE id = ? AND estado_solicitud = 'pendiente'", m, usuario, solicitud);
        if (n == 0) {
            throw new ErrorUsuario("La solicitud ya fue revisada.");
        }
    }

    /** Venta de un plan pagado en el local: se registra la solicitud y se aprueba en el mismo paso. */
    public long activarEnTienda(long cliente, String nombrePlan, long usuario, String nota) {
        Plan plan = planPorNombre(nombrePlan);
        return bd.enTransaccion(() -> {
            Integer existe = bd.jdbc().queryForObject("SELECT COUNT(*) FROM cliente WHERE id = ? AND cuenta_activa = TRUE", Integer.class, cliente);
            if (existe == null || existe == 0) {
                throw new ErrorUsuario("El cliente no existe.");
            }
            bd.jdbc().update("INSERT INTO solicitud_suscripcion (cliente_id, plan_id, metodo_pago_id, origen_solicitud, monto_pagado, fecha_pago, nota_solicitud) "
                    + "VALUES (?, ?, (SELECT id FROM metodo_pago WHERE codigo_metodo_pago = 'tienda'), 'tienda', ?, CURDATE(), ?)",
                cliente, plan.id(), plan.precio(), vacio(nota, 255) == null ? "Pago en la droguería" : vacio(nota, 255));
            long solicitud = bd.jdbc().queryForObject("SELECT MAX(id) FROM solicitud_suscripcion WHERE cliente_id = ?", Long.class, cliente);
            return aprobar(solicitud, usuario);
        });
    }

    private Plan planPorNombre(String nombre) {
        return planes().stream().filter(p -> p.nombre().equalsIgnoreCase(nombre == null ? "" : nombre.strip())).findFirst()
            .orElseThrow(() -> new ErrorUsuario("Ese plan no existe."));
    }

    private static String vacio(String v, int maximo) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String t = v.strip();
        return t.length() > maximo ? t.substring(0, maximo) : t;
    }
}
