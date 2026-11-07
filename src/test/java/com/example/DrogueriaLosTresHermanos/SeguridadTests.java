package com.example.DrogueriaLosTresHermanos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.db.url=",
    "app.admin.correo=admin@prueba.test",
    "app.admin.clave=clave-de-prueba-segura",
    "app.datos=target/test-datos/seguridad-${random.uuid}.properties"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SeguridadTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void panelAdminExigeAutenticacion() throws Exception {
        mvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?retorno=admin"));
    }

    @Test
    void laEntradaDelEquipoEsLaMismaDeLosClientes() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(redirectedUrl("/login"));
        var sesion = new org.springframework.mock.web.MockHttpSession();
        mvc.perform(post("/api/cuenta/ingreso").session(sesion).with(csrf()).contentType("application/json")
                .content("{\"correo\":\"admin@prueba.test\",\"clave\":\"clave-de-prueba-segura\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"equipo\":true")));
    }

    @Test
    void claveDelEquipoIncorrectaDaElMismoMensajeQueUnCliente() throws Exception {
        mvc.perform(post("/api/cuenta/ingreso").with(csrf()).contentType("application/json")
                .content("{\"correo\":\"admin@prueba.test\",\"clave\":\"clave-incorrecta\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("El correo o la contraseña no son correctos.")));
    }

    @Test
    void apiDelPanelSinSesionResponde401() throws Exception {
        mvc.perform(get("/api/admin/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinBaseDeDatosLasCuentasNoEstanDisponibles() throws Exception {
        mvc.perform(get("/api/cuenta"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"disponible\":false")));
        mvc.perform(post("/api/cuenta/ingreso").with(csrf()).contentType("application/json").content("{\"correo\":\"a@b.co\",\"clave\":\"12345678\"}"))
            .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/login")).andExpect(content().string(containsString("CUENTA_INICIAL")));
    }

    @Test
    void paginasPublicasSiguenAccesibles() throws Exception {
        for (String ruta : new String[] {"/", "/login", "/registro", "/medicamentos"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk());
        }
    }

    @Test
    void loginCorrectoEntraAlPanel() throws Exception {
        mvc.perform(post("/admin/login").with(csrf())
                .param("correo", "admin@prueba.test")
                .param("clave", "clave-de-prueba-segura"))
            .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginSinTokenCsrfEsRechazado() throws Exception {
        mvc.perform(post("/admin/login")
                .param("correo", "admin@prueba.test")
                .param("clave", "clave-de-prueba-segura"))
            .andExpect(status().isForbidden());
    }

    @Test
    void cincoFallosBloqueanInclusoConClaveCorrecta() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/admin/login").with(csrf())
                    .param("correo", "admin@prueba.test")
                    .param("clave", "clave-incorrecta"))
                .andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(post("/admin/login").with(csrf())
                .param("correo", "admin@prueba.test")
                .param("clave", "clave-de-prueba-segura"))
            .andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    void respuestasIncluyenCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/"))
            .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void loginAdminNoExponeCredencialesEnElHtml() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(content().string(not(containsString("lth_admins"))))
            .andExpect(content().string(not(containsString("admin@prueba.test"))))
            .andExpect(content().string(not(containsString("clave-de-prueba-segura"))));
    }
}
