package com.example.DrogueriaLosTresHermanos.servicio;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Puente con las cuentas del equipo. Mientras las cuentas sigan en el archivo de accesos,
 * cada empleado que hace una operación queda también en la tabla usuario para que MySQL
 * registre quién la hizo. La contraseña nunca se copia: el acceso se valida con el archivo.
 */
@Service
public class EquipoEnBaseDatos {

    private static final String SIN_ACCESO = "!gestionada-en-el-archivo-de-accesos";

    private final BaseDatos bd;
    private final DirectorioAccesos directorio;

    public EquipoEnBaseDatos(BaseDatos bd, DirectorioAccesos directorio) {
        this.bd = bd;
        this.directorio = directorio;
    }

    /** Identificador del empleado en la tabla usuario; lo crea si todavía no existe. */
    public synchronized long idDe(String correo) {
        String c = correo.strip().toLowerCase(Locale.ROOT);
        List<Long> existente = bd.jdbc().queryForList("SELECT id FROM usuario WHERE correo_usuario = ?", Long.class, c);
        if (!existente.isEmpty()) {
            return existente.get(0);
        }
        String rolId = directorio.cuentas().stream().filter(x -> x.correo().equals(c)).map(DirectorioAccesos.Cuenta::rolId)
            .findFirst().orElse("ADMINISTRADOR");
        String codigo = rolId.toLowerCase(Locale.ROOT);
        List<Long> rol = bd.jdbc().queryForList("SELECT id FROM rol WHERE codigo_rol = ?", Long.class, codigo);
        long rolBd;
        if (rol.isEmpty()) {
            bd.jdbc().update("INSERT INTO rol (codigo_rol, nombre_rol, descripcion_rol) VALUES (?, ?, 'Rol creado desde el panel')",
                codigo, directorio.nombreRol(rolId));
            rolBd = bd.jdbc().queryForObject("SELECT id FROM rol WHERE codigo_rol = ?", Long.class, codigo);
        } else {
            rolBd = rol.get(0);
        }
        bd.jdbc().update("INSERT INTO usuario (rol_id, nombre_usuario, correo_usuario, clave_hash) VALUES (?, ?, ?, ?)",
            rolBd, c, c, SIN_ACCESO);
        return bd.jdbc().queryForObject("SELECT id FROM usuario WHERE correo_usuario = ?", Long.class, c);
    }
}
