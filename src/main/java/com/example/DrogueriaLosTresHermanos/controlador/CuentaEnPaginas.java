package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.servicio.ServicioClientes;
import com.example.DrogueriaLosTresHermanos.servicio.ServicioSuscripciones;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pone en cada página de la tienda el estado de la cuenta del cliente: si las cuentas están disponibles,
 * quién inició sesión y el descuento de su plan. La página lo usa al cargar, sin esperar otra petición.
 */
@ControllerAdvice(assignableTypes = PaginaControlador.class)
public class CuentaEnPaginas {

    private static final Logger LOG = LoggerFactory.getLogger(CuentaEnPaginas.class);

    private final ServicioClientes clientes;
    private final ServicioSuscripciones suscripciones;

    public CuentaEnPaginas(ServicioClientes clientes, ServicioSuscripciones suscripciones) {
        this.clientes = clientes;
        this.suscripciones = suscripciones;
    }

    @ModelAttribute("cuentaInicial")
    public Map<String, Object> cuentaInicial(HttpServletRequest peticion, Authentication autenticacion) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("disponible", false);
        // Alguien del equipo con sesión abierta: la tienda le muestra el acceso al panel
        if (autenticacion != null && autenticacion.isAuthenticated() && !(autenticacion instanceof AnonymousAuthenticationToken)) {
            r.put("equipo", true);
        }
        if (!clientes.disponible()) {
            return r;
        }
        r.put("disponible", true);
        HttpSession sesion = peticion.getSession(false);
        Object id = sesion == null ? null : sesion.getAttribute(ApiCuentaControlador.SESION);
        if (!(id instanceof Long cliente)) {
            return r;
        }
        try {
            clientes.obtener(cliente).ifPresentOrElse(c -> {
                Map<String, Object> v = new LinkedHashMap<>();
                v.put("nombre", c.nombre());
                v.put("correo", c.correo());
                v.put("telefono", c.telefono());
                v.put("direccion", c.direccion());
                v.put("foto", c.foto() != null);
                r.put("cliente", v);
                suscripciones.vigente(cliente).ifPresent(s -> {
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("plan", s.plan());
                    p.put("descuento", s.descuento());
                    p.put("inicio", s.inicio());
                    p.put("vence", s.vence());
                    p.put("estado", s.estado());
                    p.put("domicilioGratis", s.domicilioGratis());
                    r.put("suscripcion", p);
                });
            }, () -> sesion.removeAttribute(ApiCuentaControlador.SESION));
        } catch (DataAccessException e) {
            LOG.warn("No se pudo leer la cuenta del cliente para la página: {}", e.getMessage());
            r.put("disponible", false);
        }
        return r;
    }
}
