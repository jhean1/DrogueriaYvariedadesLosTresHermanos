package com.example.DrogueriaLosTresHermanos.configuracion;

import com.example.DrogueriaLosTresHermanos.servicio.AlmacenCatalogo;
import com.example.DrogueriaLosTresHermanos.servicio.AlmacenCatalogoArchivo;
import com.example.DrogueriaLosTresHermanos.servicio.AlmacenCatalogoMysql;
import com.example.DrogueriaLosTresHermanos.servicio.BaseDatos;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Conexión con MySQL. Si app.db.url está vacía (pruebas automáticas o copia estática del sitio),
 * el catálogo usa el archivo local de siempre y las cuentas, pedidos y suscripciones no están disponibles.
 */
@Configuration
public class BaseDatosConfig {

    @Bean(destroyMethod = "close")
    public ConexionMysql conexionMysql(@Value("${app.db.url:}") String url, @Value("${app.db.usuario:}") String usuario,
                                       @Value("${app.db.clave:}") String clave) {
        return new ConexionMysql(url, usuario, clave);
    }

    @Bean
    public BaseDatos baseDatos(ConexionMysql conexion) {
        return new BaseDatos(conexion.origen());
    }

    @Bean
    public AlmacenCatalogo almacenCatalogo(ConexionMysql conexion, @Value("${app.catalogo}") String archivo) {
        return conexion.origen() == null ? new AlmacenCatalogoArchivo(archivo) : new AlmacenCatalogoMysql(conexion.origen());
    }

    /** Grupo de conexiones; se cierra al apagar la aplicación. */
    public static class ConexionMysql implements AutoCloseable {
        private final HikariDataSource origen;

        ConexionMysql(String url, String usuario, String clave) {
            if (!StringUtils.hasText(url)) {
                origen = null;
                return;
            }
            HikariConfig c = new HikariConfig();
            c.setJdbcUrl(url);
            c.setUsername(usuario);
            c.setPassword(clave);
            c.setMaximumPoolSize(10);
            c.setPoolName("drogueria");
            origen = new HikariDataSource(c);
        }

        public HikariDataSource origen() {
            return origen;
        }

        @Override
        public void close() {
            if (origen != null) {
                origen.close();
            }
        }
    }
}
