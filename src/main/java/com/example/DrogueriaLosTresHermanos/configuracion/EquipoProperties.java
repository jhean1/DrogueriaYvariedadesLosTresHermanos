package com.example.DrogueriaLosTresHermanos.configuracion;

import com.example.DrogueriaLosTresHermanos.modelo.RolSistema;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Miembros adicionales del equipo, definidos por variables de entorno. Por ejemplo:
 * APP_EQUIPO_0_CORREO, APP_EQUIPO_0_ROL=CAJERO y APP_EQUIPO_0_CLAVE.
 */
@ConfigurationProperties("app")
public record EquipoProperties(List<Miembro> equipo) {

    public EquipoProperties {
        equipo = equipo == null ? List.of() : List.copyOf(equipo);
    }

    public record Miembro(String correo, RolSistema rol, String clave) {}
}
