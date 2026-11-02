package com.example.DrogueriaLosTresHermanos.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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
 * Protege el panel /admin con sesión de servidor, credenciales fuera del código y
 * permisos por ruta. Las demás rutas siguen siendo públicas porque hoy las cuentas de
 * clientes, pedidos y suscripciones viven en el navegador.
 */
@Configuration
@EnableConfigurationProperties(EquipoProperties.class)
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
            .authorizeHttpRequests(rutas -> {
                rutas.requestMatchers("/admin/login").permitAll();
                rutas.requestMatchers("/admin/accesos").hasAuthority(Permiso.ROLES_GESTIONAR.getCodigo());
                for (ModuloGestion modulo : ModuloGestion.values()) {
                    rutas.requestMatchers(modulo.getRuta()).hasAuthority(modulo.getPermiso().getCodigo());
                }
                rutas.requestMatchers("/admin", "/admin/**").hasAuthority(Permiso.DASHBOARD_VER.getCodigo());
                rutas.anyRequest().permitAll();
            })
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
                                     EquipoProperties equipo,
                                     PasswordEncoder codificador) {
        InMemoryUserDetailsManager usuarios = new InMemoryUserDetailsManager(
            usuario(correo, resolverClave(clave), RolSistema.PROPIETARIO, codificador));

        for (EquipoProperties.Miembro miembro : equipo.equipo()) {
            if (miembro.correo() == null || miembro.correo().isBlank() || miembro.rol() == null) {
                throw new IllegalStateException("Cada miembro del equipo necesita correo y rol.");
            }
            validarClave(miembro.clave(), "La clave de " + miembro.correo());
            usuarios.createUser(usuario(miembro.correo(), miembro.clave(), miembro.rol(), codificador));
        }
        return usuarios;
    }

    private static UserDetails usuario(String correo, String clave, RolSistema rol, PasswordEncoder codificador) {
        return User.withUsername(correo)
            .password(codificador.encode(clave))
            .authorities(rol.autoridades())
            .build();
    }

    private static String resolverClave(String clave) {
        if (clave == null || clave.isBlank()) {
            String generada = generarClave();
            log.warn("ADMIN_CLAVE no está definida. Clave temporal del administrador (cambia en cada arranque): {}",
                generada);
            return generada;
        }
        validarClave(clave, "ADMIN_CLAVE");
        return clave;
    }

    private static void validarClave(String clave, String nombre) {
        if (clave == null || clave.length() < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException(
                nombre + " debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT) {
            throw new IllegalStateException(
                nombre + " no puede superar " + LONGITUD_MAXIMA_BCRYPT + " bytes.");
        }
    }

    private static String generarClave() {
        byte[] aleatorio = new byte[18];
        new SecureRandom().nextBytes(aleatorio);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
    }
}
