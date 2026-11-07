package com.example.DrogueriaLosTresHermanos;

import com.example.DrogueriaLosTresHermanos.servicio.DirectorioAccesos;
import com.example.DrogueriaLosTresHermanos.servicio.DirectorioAccesos.Actor;
import com.example.DrogueriaLosTresHermanos.servicio.DirectorioAccesos.OperacionNoPermitida;
import com.example.DrogueriaLosTresHermanos.modelo.ModuloGestion;
import com.example.DrogueriaLosTresHermanos.modelo.Permiso;
import com.example.DrogueriaLosTresHermanos.modelo.RolSistema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.db.url=",
    "app.admin.correo=dueno@prueba.test",
    "app.admin.clave=clave-del-propietario",
    "app.equipo[0].correo=caja@prueba.test",
    "app.equipo[0].rol=CAJERO",
    "app.equipo[0].clave=clave-de-la-caja-1",
    "app.datos=target/test-datos/roles-${random.uuid}.properties"
})
@AutoConfigureMockMvc
class RolesTests {

    private static final String CLAVE = "clave-de-prueba-12345";
    private static final Actor DUENO = new Actor("dueno@prueba.test",
        Stream.of(Permiso.values()).map(Permiso::getCodigo).collect(Collectors.toSet()), RolSistema.PROPIETARIO.name());

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DirectorioAccesos directorio;

    @BeforeEach
    void crearCuentasDePrueba() {
        for (RolSistema rol : RolSistema.values()) {
            if (rol != RolSistema.PROPIETARIO && directorio.cuentas().stream().noneMatch(c -> c.correo().equals(correoDe(rol)))) {
                directorio.crearCuenta(DUENO, correoDe(rol), CLAVE, rol.name());
            }
        }
    }

    private static String correoDe(RolSistema rol) {
        return rol == RolSistema.PROPIETARIO ? "dueno@prueba.test" : rol.name().toLowerCase(Locale.ROOT) + "@prueba.test";
    }

    private static RequestPostProcessor como(RolSistema rol) {
        return user(correoDe(rol));
    }

    private boolean rolTiene(RolSistema rol, Permiso permiso) {
        return directorio.roles().stream().filter(r -> r.id().equals(rol.name())).findFirst().orElseThrow().tiene(permiso.getCodigo());
    }

    private String idDe(String correo) {
        return directorio.cuentas().stream().filter(c -> c.correo().equals(correo)).findFirst().orElseThrow().id();
    }

    private Actor actorDe(RolSistema rol) {
        Set<String> permisos = directorio.loadUserByUsername(correoDe(rol)).getAuthorities().stream()
            .map(a -> a.getAuthority()).filter(a -> !a.startsWith("ROLE_")).collect(Collectors.toSet());
        return new Actor(correoDe(rol), permisos, rol.name());
    }

    private Set<String> autoridades(String correo) {
        UserDetails d = directorio.loadUserByUsername(correo);
        return d.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
    }

    @Test
    void propietarioAbreTodosLosModulosYAccesos() throws Exception {
        mvc.perform(get("/admin").with(como(RolSistema.PROPIETARIO))).andExpect(status().isOk());
        mvc.perform(get("/admin/accesos").with(como(RolSistema.PROPIETARIO))).andExpect(status().isOk());
        for (ModuloGestion modulo : ModuloGestion.values()) {
            mvc.perform(get(modulo.getRuta()).with(como(RolSistema.PROPIETARIO))).andExpect(status().isOk());
        }
    }

    @Test
    void cadaRolSoloAbreLosModulosQueLeCorresponden() throws Exception {
        for (RolSistema rol : RolSistema.values()) {
            for (ModuloGestion modulo : ModuloGestion.values()) {
                var esperado = rolTiene(rol, modulo.getPermiso()) ? status().isOk() : status().isForbidden();
                mvc.perform(get(modulo.getRuta()).with(como(rol))).andExpect(esperado);
            }
        }
    }

    @Test
    void soloQuienGestionaAccesosLosVe() throws Exception {
        for (RolSistema rol : RolSistema.values()) {
            var esperado = rol == RolSistema.PROPIETARIO ? status().isOk() : status().isForbidden();
            mvc.perform(get("/admin/accesos").with(como(rol))).andExpect(esperado);
        }
    }

    @Test
    void elPanelDelCajeroNoIncluyeClientesNiPagos() throws Exception {
        mvc.perform(get("/admin").with(como(RolSistema.CAJERO)))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("id=\"sec-clientes\""))))
            .andExpect(content().string(not(containsString("id=\"sec-solicitudes\""))))
            .andExpect(content().string(not(containsString("/admin/accesos"))));
    }

    @Test
    void elPanelDelAdministradorIncluyeClientesYPagos() throws Exception {
        mvc.perform(get("/admin").with(como(RolSistema.ADMINISTRADOR)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"sec-clientes\"")))
            .andExpect(content().string(containsString("id=\"sec-solicitudes\"")));
    }

    @Test
    void moduloInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/admin/modulos/no-existe").with(como(RolSistema.PROPIETARIO)))
            .andExpect(status().isNotFound());
    }

    @Test
    void elEquipoSeCargaDesdeLaConfiguracion() {
        Set<String> cajero = autoridades("caja@prueba.test");
        assertTrue(cajero.contains("sales.create"));
        assertFalse(cajero.contains("users.manage"));
    }

    @Test
    void lasAccionesDeAccesosExigenPermisoYCsrf() throws Exception {
        mvc.perform(post("/admin/accesos/roles").with(csrf()).with(como(RolSistema.CAJERO)).param("nombre", "Intruso"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/accesos/roles").with(como(RolSistema.PROPIETARIO)).param("nombre", "Sin token"))
            .andExpect(status().isForbidden());
        assertTrue(directorio.roles().stream().noneMatch(r -> r.nombre().equals("Intruso") || r.nombre().equals("Sin token")));
    }

    @Test
    void propietarioCreaUnRolYLeAsignaPermisos() throws Exception {
        mvc.perform(post("/admin/accesos/roles").with(csrf()).with(como(RolSistema.PROPIETARIO)).param("nombre", "Domiciliario"))
            .andExpect(redirectedUrl("/admin/accesos"));
        String id = directorio.roles().stream().filter(r -> r.nombre().equals("Domiciliario")).findFirst().orElseThrow().id();

        mvc.perform(post("/admin/accesos/roles/" + id + "/permisos").with(csrf()).with(como(RolSistema.PROPIETARIO))
                .param("permisos", "dashboard.view", "sales.view", "permiso.inventado"))
            .andExpect(flash().attribute("mensaje", "Permisos del rol actualizados."));

        assertEquals(Set.of("dashboard.view", "sales.view"),
            directorio.roles().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow().permisos());
    }

    @Test
    void revocarUnPermisoTieneEfectoInmediato() throws Exception {
        directorio.crearCuenta(DUENO, "temporal1@prueba.test", CLAVE, RolSistema.CAJERO.name());
        mvc.perform(get("/admin/modulos/ventas").with(user("temporal1@prueba.test"))).andExpect(status().isOk());

        directorio.cambiarPermisosCuenta(DUENO, idDe("temporal1@prueba.test"), List.of(), List.of("sales.view"));

        mvc.perform(get("/admin/modulos/ventas").with(user("temporal1@prueba.test"))).andExpect(status().isForbidden());
    }

    @Test
    void desactivarUnaCuentaLaExpulsaDeInmediato() throws Exception {
        directorio.crearCuenta(DUENO, "temporal2@prueba.test", CLAVE, RolSistema.CAJERO.name());
        directorio.cambiarEstadoCuenta(DUENO, idDe("temporal2@prueba.test"), false);

        mvc.perform(get("/admin").with(user("temporal2@prueba.test"))).andExpect(redirectedUrl("/login?inactiva"));
        assertFalse(directorio.loadUserByUsername("temporal2@prueba.test").isEnabled());
    }

    @Test
    void nadiePuedeEscalarPrivilegios() {
        Actor admin = actorDe(RolSistema.ADMINISTRADOR);

        // Un administrador no puede asignar el rol de propietario.
        assertThrows(OperacionNoPermitida.class,
            () -> directorio.crearCuenta(admin, "nuevo@prueba.test", CLAVE, RolSistema.PROPIETARIO.name()));
        // Nadie modifica su propia cuenta ni su propio rol.
        assertThrows(OperacionNoPermitida.class,
            () -> directorio.cambiarPermisosCuenta(admin, idDe(admin.correo()), List.of("users.manage"), List.of()));
        assertThrows(OperacionNoPermitida.class,
            () -> directorio.cambiarPermisosRol(admin, RolSistema.ADMINISTRADOR.name(), List.of("users.manage")));
        // El rol y la cuenta del propietario son intocables.
        assertThrows(OperacionNoPermitida.class,
            () -> directorio.cambiarPermisosRol(DUENO, RolSistema.PROPIETARIO.name(), List.of()));
        assertThrows(OperacionNoPermitida.class,
            () -> directorio.cambiarEstadoCuenta(DUENO, "propietario", false));

        // Un permiso que el actor no tiene se ignora, aunque lo envíe a mano.
        directorio.cambiarPermisosRol(admin, RolSistema.CAJERO.name(), List.of("users.manage", "dashboard.view"));
        assertFalse(directorio.roles().stream().filter(r -> r.id().equals(RolSistema.CAJERO.name())).findFirst().orElseThrow().tiene("users.manage"));
        // ...y los que tiene el rol pero no el actor se conservan.
        assertTrue(directorio.roles().stream().filter(r -> r.id().equals(RolSistema.CAJERO.name())).findFirst().orElseThrow().tiene("cash.operate"));
        // Restablece lo que cambió esta prueba.
        directorio.cambiarPermisosRol(DUENO, RolSistema.CAJERO.name(),
            RolSistema.CAJERO.getPermisos().stream().map(Permiso::getCodigo).toList());
    }

    @Test
    void losCambiosQuedanEnLaAuditoria() throws Exception {
        directorio.crearCuenta(DUENO, "auditada@prueba.test", CLAVE, RolSistema.AUXILIAR.name());
        assertTrue(directorio.eventos().stream().anyMatch(e -> e.accion().equals("Cuenta creada") && e.detalle().contains("auditada@prueba.test")));
        mvc.perform(get("/admin/modulos/auditoria").with(como(RolSistema.PROPIETARIO)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("auditada@prueba.test")));
    }
}
