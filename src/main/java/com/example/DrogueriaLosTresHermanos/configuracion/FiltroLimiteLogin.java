package com.example.DrogueriaLosTresHermanos.configuracion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Rechaza el POST de login antes de comprobar la clave si la IP está bloqueada. */
class FiltroLimiteLogin extends OncePerRequestFilter {

    private final LimiteIntentosLogin limite;

    FiltroLimiteLogin(LimiteIntentosLogin limite) {
        this.limite = limite;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        boolean esLogin = "POST".equals(peticion.getMethod())
            && (peticion.getContextPath() + "/admin/login").equals(peticion.getRequestURI());

        if (esLogin && limite.estaBloqueada(peticion.getRemoteAddr())) {
            respuesta.sendRedirect(peticion.getContextPath() + "/admin/login?bloqueado");
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }
}
