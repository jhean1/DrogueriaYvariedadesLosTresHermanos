package com.example.DrogueriaLosTresHermanos.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Seguridad del lado del servidor.
 *
 * Protege el panel /admin con sesión de servidor y credenciales fuera del código.
 * Las demás rutas siguen siendo públicas porque hoy las cuentas de clientes,
 * pedidos y suscripciones viven en el navegador; no hay datos que proteger aquí.
 */
@Configuration
public class SeguridadConfig {

    private static final Logger log = LoggerFactory.getLogger(SeguridadConfig.class);

    private static final int LONGITUD_MINIMA_CLAVE = 12;
    private static final int LONGITUD_MAXIMA_BCRYPT = 72;

    // 'unsafe-inline' es necesario mientras las plantillas usen <script> y onclick
    // en línea. Aun así, la política impide cargar scripts externos y enviar datos
    // a otros dominios por fetch/XHR.
    private static final String POLITICA_CONTENIDO = String.join("; ",
        "default-src 'self'",
        "script-src 'self' 'unsafe-inline'",
        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
        "font-src 'self' https://fonts.gstatic.com",
        "img-src 'self' data:",
        "frame-src https://www.google.com",
        "connect-src 'self'",
        "object-src 'none'",
        "base-uri 'self'",
        "form-action 'self'",
        "frame-ancestors 'none'");

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, LimiteIntentosLogin limite) throws Exception {
        http
            .authorizeHttpRequests(rutas -> rutas
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .formLogin(login -> login
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .usernameParameter("correo")
                .passwordParameter("clave")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/login?error"))
            .logout(salida -> salida
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?salir")
                .deleteCookies("JSESSIONID"))
            .headers(cabeceras -> cabeceras
                .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_CONTENIDO))
                .frameOptions(marco -> marco.deny())
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .addFilterBefore(new FiltroLimiteLogin(limite), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuariosAdmin(@Value("${app.admin.correo}") String correo,
                                     @Value("${app.admin.clave:}") String clave,
                                     PasswordEncoder codificador) {
        return new InMemoryUserDetailsManager(User.withUsername(correo)
            .password(codificador.encode(resolverClave(clave)))
            .roles("ADMIN")
            .build());
    }

    private static String resolverClave(String clave) {
        if (clave == null || clave.isBlank()) {
            String generada = generarClave();
            log.warn("ADMIN_CLAVE no está definida. Clave temporal del administrador (cambia en cada arranque): {}",
                generada);
            return generada;
        }
        if (clave.length() < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException(
                "ADMIN_CLAVE debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT) {
            throw new IllegalStateException(
                "ADMIN_CLAVE no puede superar " + LONGITUD_MAXIMA_BCRYPT + " bytes.");
        }
        return clave;
    }

    private static String generarClave() {
        byte[] aleatorio = new byte[18];
        new SecureRandom().nextBytes(aleatorio);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
    }
}
