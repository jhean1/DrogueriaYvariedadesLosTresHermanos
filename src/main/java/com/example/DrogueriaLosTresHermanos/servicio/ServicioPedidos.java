package com.example.DrogueriaLosTresHermanos.servicio;

import com.example.DrogueriaLosTresHermanos.servicio.BaseDatos.ErrorUsuario;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pedidos de la tienda en línea. Precios, descuento y total se calculan aquí con los datos de MySQL:
 * lo que envía el navegador solo indica qué productos y cuántos.
 */
@Service
public class ServicioPedidos {

    /** Una línea del carrito: la presentación de un medicamento, o el producto si es una variedad. */
    public record Linea(Long presentacion, Long producto, int cantidad) {}

    /**
     * Valor del domicilio. Sin ubicación del cliente, la distancia queda vacía y la droguería confirma el valor.
     * Con un plan que incluye domicilio gratis, el costo es cero.
     */
    public record Domicilio(Double distanciaKm, int costo, boolean gratis, boolean conUbicacion, int maximoKm) {}

    private static final int MAX_LINEAS = 40;
    private static final int MAX_CANTIDAD = 50;
    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    private final BaseDatos bd;
    private final ServicioSuscripciones suscripciones;

    public ServicioPedidos(BaseDatos bd, ServicioSuscripciones suscripciones) {
        this.bd = bd;
        this.suscripciones = suscripciones;
    }

    /** Calcula el domicilio desde la droguería hasta la ubicación del cliente (en línea recta). */
    public Domicilio cotizarDomicilio(Long cliente, Double latitud, Double longitud) {
        Map<String, String> c = new java.util.HashMap<>();
        bd.jdbc().query("SELECT clave_configuracion, valor_configuracion FROM configuracion WHERE clave_configuracion LIKE 'domicilio\\_%' "
            + "OR clave_configuracion IN ('tienda_latitud', 'tienda_longitud')", r -> {
                c.put(r.getString(1), r.getString(2));
            });
        int base = entero(c.get("domicilio_tarifa_base"), 3000);
        double incluidos = decimal(c.get("domicilio_km_incluidos"), 1);
        int porKm = entero(c.get("domicilio_valor_km"), 1000);
        int maximo = entero(c.get("domicilio_max_km"), 8);
        boolean gratis = cliente != null && suscripciones.vigente(cliente).map(ServicioSuscripciones.Vigente::domicilioGratis).orElse(false);
        if (latitud == null || longitud == null) {
            return new Domicilio(null, 0, gratis, false, maximo);
        }
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw new ErrorUsuario("La ubicación no es válida.");
        }
        double km = distanciaKm(decimal(c.get("tienda_latitud"), 10.2572520), decimal(c.get("tienda_longitud"), -75.3366076), latitud, longitud);
        km = Math.round(km * 100) / 100.0;
        if (km > maximo) {
            throw new ErrorUsuario("Tu ubicación está a " + String.format(java.util.Locale.ROOT, "%.1f", km).replace('.', ',')
                + " km de la droguería. Hacemos domicilios hasta " + maximo + " km; escríbenos por WhatsApp para coordinar la entrega.");
        }
        int costo = gratis ? 0 : base + (int) Math.ceil(Math.max(0, km - incluidos)) * porKm;
        return new Domicilio(km, costo, gratis, true, maximo);
    }

    /** Distancia en línea recta entre dos puntos (fórmula del haversine). */
    static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static int entero(String v, int porDefecto) {
        try {
            return v == null ? porDefecto : Integer.parseInt(v.strip());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private static double decimal(String v, double porDefecto) {
        try {
            return v == null ? porDefecto : Double.parseDouble(v.strip());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    /** Crea el pedido y devuelve su número. */
    public String crear(long cliente, List<Linea> lineas, String modalidad, String direccion, Double latitud, Double longitud) {
        if (lineas == null || lineas.isEmpty()) {
            throw new ErrorUsuario("Tu carrito está vacío.");
        }
        if (lineas.size() > MAX_LINEAS) {
            throw new ErrorUsuario("El pedido tiene demasiados productos distintos.");
        }
        if (!"tienda".equals(modalidad) && !"domicilio".equals(modalidad)) {
            throw new ErrorUsuario("Elige cómo quieres recibir tu pedido.");
        }
        String dir = direccion == null ? "" : direccion.strip();
        if ("domicilio".equals(modalidad) && (dir.isEmpty() || dir.length() > 255)) {
            throw new ErrorUsuario("Escribe la dirección de entrega.");
        }
        // Une las líneas repetidas de una misma presentación
        return bd.enTransaccion(() -> {
            Map<Long, Integer> cantidades = new LinkedHashMap<>();
            for (Linea l : lineas) {
                if (l.cantidad() < 1 || l.cantidad() > MAX_CANTIDAD) {
                    throw new ErrorUsuario("Cada producto debe tener entre 1 y " + MAX_CANTIDAD + " unidades.");
                }
                cantidades.merge(presentacionDe(l), l.cantidad(), Integer::sum);
            }
            var vigente = suscripciones.vigente(cliente);
            int pct = vigente.map(ServicioSuscripciones.Vigente::descuento).orElse(0);
            Domicilio envio = "domicilio".equals(modalidad) ? cotizarDomicilio(cliente, latitud, longitud) : null;
            BigDecimal domicilio = envio == null ? BigDecimal.ZERO : BigDecimal.valueOf(envio.costo());
            record Fila(long presentacion, String nombre, int cantidad, int original, int cobrado) {}
            List<Fila> filas = new ArrayList<>();
            for (var e : cantidades.entrySet()) {
                List<Map<String, Object>> p = bd.jdbc().queryForList("SELECT pr.precio_venta, CONCAT(p.nombre_producto, ' - ', pr.tipo_presentacion, "
                        + "IFNULL(CONCAT(' ', pr.marca), '')) AS nombre FROM presentacion pr JOIN producto p ON p.id = pr.producto_id "
                        + "WHERE pr.id = ? AND pr.presentacion_activa = TRUE AND p.producto_activo = TRUE", e.getKey());
                if (p.isEmpty()) {
                    throw new ErrorUsuario("Uno de los productos del carrito ya no está disponible. Revisa tu carrito.");
                }
                int original = ((BigDecimal) p.get(0).get("precio_venta")).intValue();
                int cobrado = (int) Math.round(original * (100 - pct) / 100.0);
                String nombre = (String) p.get(0).get("nombre");
                filas.add(new Fila(e.getKey(), nombre.length() > 255 ? nombre.substring(0, 255) : nombre, e.getValue(), original, cobrado));
            }
            long subtotal = filas.stream().mapToLong(f -> (long) f.original() * f.cantidad()).sum();
            long descuento = filas.stream().mapToLong(f -> (long) (f.original() - f.cobrado()) * f.cantidad()).sum();
            String numero = null;
            for (int intento = 0; numero == null && intento < 5; intento++) {
                String candidato = "LTH-" + codigo(6);
                try {
                    bd.jdbc().update("INSERT INTO pedido (numero_pedido, canal_venta, cliente_id, suscripcion_id, modalidad_entrega, direccion_entrega, "
                            + "subtotal_pedido, descuento_porcentaje, descuento_aplicado, costo_domicilio, distancia_entrega_km) VALUES (?, 'web', ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        candidato, cliente, pct > 0 || (envio != null && envio.gratis()) ? vigente.map(ServicioSuscripciones.Vigente::id).orElse(null) : null,
                        modalidad, "domicilio".equals(modalidad) ? dir : null,
                        subtotal, pct, descuento, domicilio, envio == null ? null : envio.distanciaKm());
                    numero = candidato;
                } catch (DuplicateKeyException repetido) {
                    // Número ya usado: se intenta con otro
                }
            }
            if (numero == null) {
                throw new IllegalStateException("No se pudo asignar un número de pedido");
            }
            long pedido = bd.jdbc().queryForObject("SELECT id FROM pedido WHERE numero_pedido = ?", Long.class, numero);
            for (Fila f : filas) {
                bd.jdbc().update("INSERT INTO detalle_pedido (pedido_id, presentacion_id, nombre_producto, cantidad_vendida, precio_original, precio_unitario) "
                    + "VALUES (?, ?, ?, ?, ?, ?)", pedido, f.presentacion(), f.nombre(), f.cantidad(), f.original(), f.cobrado());
            }
            return numero;
        });
    }

    public List<Map<String, Object>> deCliente(long cliente) {
        List<Map<String, Object>> pedidos = bd.jdbc().queryForList(resumenSql() + " WHERE p.cliente_id = ? ORDER BY p.fecha_creacion DESC", cliente);
        pedidos.forEach(p -> p.put("productos", lineas(((Number) p.get("id")).longValue())));
        return pedidos;
    }

    public Optional<Map<String, Object>> deClientePorNumero(long cliente, String numero) {
        return bd.jdbc().queryForList(resumenSql() + " WHERE p.cliente_id = ? AND p.numero_pedido = ?", cliente, numero).stream().findFirst()
            .map(p -> {
                p.put("productos", lineas(((Number) p.get("id")).longValue()));
                return p;
            });
    }

    // ── Panel ─────────────────────────────────────────────────

    public List<Map<String, Object>> paraPanel() {
        List<Map<String, Object>> pedidos = bd.jdbc().queryForList(resumenSql() + " ORDER BY p.estado_pedido IN ('pendiente','en_proceso') DESC, p.fecha_creacion DESC LIMIT 500");
        pedidos.forEach(p -> p.put("productos", lineas(((Number) p.get("id")).longValue())));
        return pedidos;
    }

    /**
     * Avanza un pedido: pendiente → en_proceso → entregado, o lo cancela.
     * Al entregarlo se descuenta el inventario de las presentaciones que ya tienen lotes registrados
     * (empezando por el que vence antes) y se registra el pago en efectivo.
     */
    public void cambiarEstado(long pedido, String nuevo, long usuario, String motivo) {
        bd.enTransaccion(() -> {
            List<String> actual = bd.jdbc().queryForList("SELECT estado_pedido FROM pedido WHERE id = ? FOR UPDATE", String.class, pedido);
            if (actual.isEmpty()) {
                throw new ErrorUsuario("El pedido no existe.");
            }
            String estado = actual.get(0);
            try {
                switch (nuevo) {
                    case "en_proceso" -> {
                        exigir(estado.equals("pendiente"), "Solo un pedido pendiente pasa a preparación.");
                        bd.jdbc().update("UPDATE pedido SET estado_pedido = 'en_proceso' WHERE id = ?", pedido);
                    }
                    case "entregado" -> {
                        exigir(estado.equals("pendiente") || estado.equals("en_proceso"), "Este pedido ya no se puede entregar.");
                        for (Map<String, Object> l : bd.jdbc().queryForList("SELECT d.id, (SELECT COUNT(*) FROM lote x WHERE x.presentacion_id = d.presentacion_id) AS lotes "
                                + "FROM detalle_pedido d WHERE d.pedido_id = ?", pedido)) {
                            if (((Number) l.get("lotes")).intValue() > 0) {
                                bd.jdbc().update("CALL descontar_stock_fefo(?, ?, NOW())", ((Number) l.get("id")).longValue(), usuario);
                            }
                        }
                        bd.jdbc().update("UPDATE pedido SET estado_pedido = 'entregado', numero_comprobante = CONCAT('CP-', LPAD(id, 6, '0')) WHERE id = ?", pedido);
                        bd.jdbc().update("INSERT INTO pago_pedido (pedido_id, metodo_pago_id, monto_pago, estado_pago) "
                            + "SELECT id, (SELECT id FROM metodo_pago WHERE codigo_metodo_pago = 'tienda'), total_pedido, 'confirmado' FROM pedido "
                            + "WHERE id = ? AND total_pedido > 0", pedido);
                    }
                    case "cancelado" -> {
                        exigir(!estado.equals("cancelado"), "El pedido ya está anulado.");
                        String m = motivo == null || motivo.isBlank() ? "Anulado desde el panel" : motivo.strip();
                        bd.jdbc().update("CALL anular_pedido(?, ?, ?)", pedido, usuario, m.length() > 255 ? m.substring(0, 255) : m);
                    }
                    default -> throw new ErrorUsuario("Estado no válido.");
                }
            } catch (DataAccessException e) {
                String regla = BaseDatos.mensajeDeRegla(e);
                if (regla != null) {
                    throw new ErrorUsuario(regla);
                }
                throw e;
            }
            return null;
        });
    }

    public Map<String, Object> resumenPanel() {
        return bd.jdbc().queryForMap("SELECT (SELECT COUNT(*) FROM cliente WHERE cuenta_activa) AS clientes, "
            + "(SELECT COUNT(*) FROM pedido WHERE estado_pedido IN ('pendiente','en_proceso')) AS pedidosAbiertos, "
            + "(SELECT COUNT(*) FROM pedido WHERE DATE(fecha_creacion) = CURDATE()) AS pedidosHoy, "
            + "(SELECT IFNULL(SUM(total_pedido), 0) FROM pedido WHERE estado_pedido = 'entregado' AND DATE(fecha_creacion) = CURDATE()) AS ventasHoy, "
            + "(SELECT COUNT(*) FROM solicitud_suscripcion WHERE estado_solicitud = 'pendiente') AS pagosPendientes, "
            + "(SELECT COUNT(*) FROM solicitud_suscripcion WHERE estado_solicitud = 'aprobado') AS pagosAprobados, "
            + "(SELECT COUNT(*) FROM solicitud_suscripcion WHERE estado_solicitud = 'rechazado') AS pagosRechazados, "
            + "(SELECT COUNT(*) FROM suscripcion WHERE estado_suscripcion = 'activa' AND fecha_vencimiento > NOW()) AS planesActivos");
    }

    private static String resumenSql() {
        return "SELECT p.id, p.numero_pedido AS numero, p.numero_comprobante AS comprobante, DATE_FORMAT(p.fecha_creacion, '%Y-%m-%d %H:%i') AS fecha, "
            + "p.modalidad_entrega AS modalidad, p.direccion_entrega AS direccion, p.subtotal_pedido AS subtotal, p.descuento_porcentaje AS descuentoPorcentaje, "
            + "p.descuento_aplicado AS descuento, p.costo_domicilio AS domicilio, p.distancia_entrega_km AS distanciaKm, "
            + "(p.modalidad_entrega = 'domicilio' AND p.costo_domicilio = 0 AND EXISTS (SELECT 1 FROM suscripcion s JOIN plan_suscripcion pl ON pl.id = s.plan_id "
            + "WHERE s.id = p.suscripcion_id AND pl.domicilio_gratis)) AS domicilioGratis, "
            + "p.total_pedido AS total, p.estado_pedido AS estado, p.canal_venta AS canal, "
            + "c.nombre_cliente AS cliente, c.telefono_cliente AS telefono FROM pedido p LEFT JOIN cliente c ON c.id = p.cliente_id";
    }

    private List<Map<String, Object>> lineas(long pedido) {
        return bd.jdbc().queryForList("SELECT d.presentacion_id AS presentacion, d.nombre_producto AS nombre, d.cantidad_vendida AS cantidad, "
            + "d.precio_original AS precioOriginal, d.precio_unitario AS precio, d.subtotal_linea AS subtotal FROM detalle_pedido d WHERE d.pedido_id = ? ORDER BY d.id", pedido);
    }

    /** Para una variedad, su única presentación activa. */
    private long presentacionDe(Linea l) {
        if (l.presentacion() != null) {
            return l.presentacion();
        }
        if (l.producto() == null) {
            throw new ErrorUsuario("Uno de los productos del carrito no es válido.");
        }
        return bd.jdbc().queryForList("SELECT id FROM presentacion WHERE producto_id = ? AND presentacion_activa = TRUE ORDER BY id LIMIT 1",
            Long.class, l.producto()).stream().findFirst()
            .orElseThrow(() -> new ErrorUsuario("Uno de los productos del carrito ya no está disponible. Revisa tu carrito."));
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new ErrorUsuario(mensaje);
        }
    }

    private static String codigo(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) {
            s.append(LETRAS.charAt(AZAR.nextInt(LETRAS.length())));
        }
        return s.toString();
    }
}
