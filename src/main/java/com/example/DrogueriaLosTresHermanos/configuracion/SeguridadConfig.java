package com.example.DrogueriaLosTresHermanos.configuracion;

import com.example.DrogueriaLosTresHermanos.modelo.ModuloGestion;
import com.example.DrogueriaLosTresHermanos.modelo.Permiso;
import com.example.DrogueriaLosTresHermanos.servicio.DirectorioAccesos;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Seguridad del lado del servidor.
 *
 * Protege el panel /admin y su API con sesión de servidor, credenciales fuera del código y
 * permisos por ruta. La API de la tienda (/api/cuenta, /api/pedidos...) exige la sesión del
 * cliente dentro de su controlador. Todas las peticiones que cambian datos llevan token CSRF.
 */
@Configuration
@EnableConfigurationProperties(EquipoProperties.class)
public class SeguridadConfig {

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
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, LimiteIntentosLogin limite, DirectorioAccesos directorio) throws Exception {
        http
            .authorizeHttpRequests(rutas -> {
                rutas.requestMatchers("/admin/login").permitAll();
                // API del panel: clientes, pagos de suscripciones y pedidos
                rutas.requestMatchers(HttpMethod.GET, "/api/admin/resumen").hasAuthority(Permiso.DASHBOARD_VER.getCodigo());
                rutas.requestMatchers(HttpMethod.GET, "/api/admin/clientes").hasAuthority(Permiso.CLIENTES_VER.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/api/admin/clientes").hasAuthority(Permiso.CLIENTES_CREAR.getCodigo());
                rutas.requestMatchers(HttpMethod.GET, "/api/admin/solicitudes", "/api/admin/solicitudes/*/comprobante")
                    .hasAuthority(Permiso.SUSCRIPCIONES_VER.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/api/admin/solicitudes/*/aprobar", "/api/admin/solicitudes/*/rechazar")
                    .hasAuthority(Permiso.SUSCRIPCIONES_APROBAR.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/api/admin/suscripciones/tienda").hasAuthority(Permiso.SUSCRIPCIONES_ACTIVAR.getCodigo());
                rutas.requestMatchers(HttpMethod.GET, "/api/admin/pedidos").hasAuthority(Permiso.VENTAS_VER.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/api/admin/pedidos/*/estado").hasAuthority(Permiso.VENTAS_VER.getCodigo());
                rutas.requestMatchers("/api/admin/**").denyAll();
                rutas.requestMatchers("/admin/productos/nuevo").hasAuthority(Permiso.PRODUCTOS_CREAR.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/admin/productos/*/visibilidad").hasAuthority(Permiso.PRODUCTOS_ELIMINAR.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/admin/productos/*/foto").hasAuthority(Permiso.PRODUCTOS_EDITAR.getCodigo());
                rutas.requestMatchers("/admin/productos/*").hasAuthority(Permiso.PRODUCTOS_EDITAR.getCodigo());
                rutas.requestMatchers(HttpMethod.GET, "/admin/productos").hasAuthority(Permiso.PRODUCTOS_VER.getCodigo());
                rutas.requestMatchers("/admin/productos/**").denyAll();
                rutas.requestMatchers("/admin/categorias", "/admin/categorias/**").hasAuthority(Permiso.PRODUCTOS_EDITAR.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/admin/accesos/roles/*/permisos", "/admin/accesos/usuarios/*/permisos")
                    .hasAuthority(Permiso.PERMISOS_GESTIONAR.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/admin/accesos/roles").hasAuthority(Permiso.ROLES_GESTIONAR.getCodigo());
                rutas.requestMatchers(HttpMethod.POST, "/admin/accesos/usuarios", "/admin/accesos/usuarios/*/rol", "/admin/accesos/usuarios/*/estado")
                    .hasAuthority(Permiso.USUARIOS_GESTIONAR.getCodigo());
                rutas.requestMatchers(HttpMethod.GET, "/admin/accesos")
                    .hasAnyAuthority(Permiso.ROLES_GESTIONAR.getCodigo(), Permiso.USUARIOS_GESTIONAR.getCodigo());
                rutas.requestMatchers("/admin/accesos/**").denyAll();
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
            // La API responde 401; las páginas del panel llevan al formulario de acceso
            .exceptionHandling(e -> e.authenticationEntryPoint(entradaSinSesion()))
            .logout(salida -> salida
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?salir")
                .deleteCookies("JSESSIONID"))
            .headers(cabeceras -> cabeceras
                .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_CONTENIDO))
                .frameOptions(marco -> marco.deny())
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
            .addFilterBefore(new FiltroLimiteLogin(limite), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new FiltroPermisosVigentes(directorio), AuthorizationFilter.class);

        return http.build();
    }

    private static AuthenticationEntryPoint entradaSinSesion() {
        RequestMatcher api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
        AuthenticationEntryPoint estado = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        AuthenticationEntryPoint formulario = new LoginUrlAuthenticationEntryPoint("/admin/login");
        return (peticion, respuesta, error) -> (api.matches(peticion) ? estado : formulario).commence(peticion, respuesta, error);
    }

    @Bean
    static PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }
}
