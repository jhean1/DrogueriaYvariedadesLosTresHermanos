package com.example.DrogueriaLosTresHermanos;

import com.example.DrogueriaLosTresHermanos.configuracion.ModuloGestion;
import com.example.DrogueriaLosTresHermanos.configuracion.RolSistema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.admin.correo=dueno@prueba.test",
    "app.admin.clave=clave-del-propietario",
    "app.equipo[0].correo=caja@prueba.test",
    "app.equipo[0].rol=CAJERO",
    "app.equipo[0].clave=clave-de-la-caja-1"
})
@AutoConfigureMockMvc
class RolesTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserDetailsService usuarios;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor como(RolSistema rol) {
        return user("persona@prueba.test").authorities(rol.autoridades());
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
                var esperado = rol.tiene(modulo.getPermiso()) ? status().isOk() : status().isForbidden();
                mvc.perform(get(modulo.getRuta()).with(como(rol))).andExpect(esperado);
            }
        }
    }

    @Test
    void soloElPropietarioVeRolesYPermisos() throws Exception {
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
        UserDetails cajero = usuarios.loadUserByUsername("caja@prueba.test");
        assertTrue(cajero.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("sales.create")));
        assertTrue(cajero.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("users.manage")));
    }
}
