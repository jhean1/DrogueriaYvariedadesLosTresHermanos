package com.example.DrogueriaLosTresHermanos.configuracion;

import com.example.DrogueriaLosTresHermanos.servicio.DirectorioAccesos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Recalcula los permisos en cada petición: revocar un permiso o desactivar una cuenta tiene
 * efecto inmediato, sin esperar a que la persona vuelva a iniciar sesión.
 */
class FiltroPermisosVigentes extends OncePerRequestFilter {

    private final DirectorioAccesos directorio;

    FiltroPermisosVigentes(DirectorioAccesos directorio) {
        this.directorio = directorio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();

        if (actual instanceof UsernamePasswordAuthenticationToken && actual.isAuthenticated()) {
            Optional<List<GrantedAuthority>> vigentes = directorio.autoridadesVigentes(actual.getName());
            if (vigentes.isEmpty()) {
                SecurityContextHolder.clearContext();
                HttpSession sesion = peticion.getSession(false);
                if (sesion != null) {
                    sesion.invalidate();
                }
                respuesta.sendRedirect(peticion.getContextPath() + "/admin/login?inactiva");
                return;
            }
            UsernamePasswordAuthenticationToken renovada = UsernamePasswordAuthenticationToken.authenticated(
                actual.getPrincipal(), null, vigentes.get());
            renovada.setDetails(actual.getDetails());
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(renovada);
            SecurityContextHolder.setContext(contexto);
        }
        cadena.doFilter(peticion, respuesta);
    }
}
