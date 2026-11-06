package com.example.DrogueriaLosTresHermanos.servicio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.function.Supplier;

/**
 * Acceso a MySQL compartido por los servicios. Si no hay conexión configurada
 * (pruebas automáticas o copia estática del sitio), las funciones que la necesitan
 * responden que el servicio no está disponible en lugar de fallar.
 */
public class BaseDatos {

    /** La página funciona sin base de datos, pero las cuentas, pedidos y suscripciones la necesitan. */
    public static class NoDisponible extends RuntimeException {
        public NoDisponible() {
            super("Este servicio no está disponible en esta versión de la página.");
        }
    }

    /** Error causado por datos del usuario; su mensaje se le puede mostrar. */
    public static class ErrorUsuario extends RuntimeException {
        public ErrorUsuario(String mensaje) {
            super(mensaje);
        }
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;

    public BaseDatos(DataSource origen) {
        this.jdbc = origen == null ? null : new JdbcTemplate(origen);
        this.transaccion = origen == null ? null : new TransactionTemplate(new DataSourceTransactionManager(origen));
    }

    public boolean disponible() {
        return jdbc != null;
    }

    public JdbcTemplate jdbc() {
        if (jdbc == null) {
            throw new NoDisponible();
        }
        return jdbc;
    }

    /** Ejecuta varias operaciones como una sola: si una falla, no queda ninguna a medias. */
    public <T> T enTransaccion(Supplier<T> trabajo) {
        if (transaccion == null) {
            throw new NoDisponible();
        }
        return transaccion.execute(estado -> trabajo.get());
    }

    /** Mensaje de un SIGNAL de MySQL (reglas de los procedimientos y disparadores), o null si no lo es. */
    public static String mensajeDeRegla(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.sql.SQLException s && "45000".equals(s.getSQLState())) {
                return s.getMessage();
            }
        }
        return null;
    }
}
