package com.example.DrogueriaLosTresHermanos;

import com.example.DrogueriaLosTresHermanos.catalogo.CatalogoProductos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.admin.correo=dueno@prueba.test",
    "app.admin.clave=clave-del-propietario",
    "app.equipo[0].correo=caja@prueba.test",
    "app.equipo[0].rol=CAJERO",
    "app.equipo[0].clave=clave-de-la-caja-1",
    "app.equipo[1].correo=regente@prueba.test",
    "app.equipo[1].rol=REGENTE",
    "app.equipo[1].clave=clave-del-regente-1",
    "app.datos=target/test-datos/productos-${random.uuid}.properties",
    "app.catalogo=target/test-datos/catalogo-${random.uuid}.properties",
    "app.fotos=target/test-datos/fotos-${random.uuid}"
})
@AutoConfigureMockMvc
class ProductosTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CatalogoProductos catalogo;

    @Test
    void elCatalogoInicialConservaLosDatosExistentes() {
        // Un catálogo nuevo: las demás pruebas de la clase modifican el compartido
        CatalogoProductos inicial = new CatalogoProductos("target/test-datos/sin-crear-" + java.util.UUID.randomUUID() + ".properties");
        assertEquals(15, inicial.visibles(CatalogoProductos.MEDICAMENTO).size());
        assertEquals(11, inicial.visibles(CatalogoProductos.VARIEDAD).size());
        assertEquals(115, inicial.visibles(CatalogoProductos.MEDICAMENTO).stream().mapToInt(p -> p.presentaciones().size()).sum());
        assertTrue(inicial.visibles(CatalogoProductos.MEDICAMENTO).stream().filter(p -> p.nombre().equals("Clonazepam")).findFirst().orElseThrow().formula());
    }

    @Test
    void elCatalogoPublicoEsUnScriptSeguro() throws Exception {
        mvc.perform(get("/js/catalogo.js"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("const MEDICAMENTOS = [")))
            .andExpect(content().string(containsString("Acetaminofén")));

        mvc.perform(multipart("/admin/productos/nuevo").with(csrf()).with(user("dueno@prueba.test"))
                .param("tipo", "variedad").param("nombre", "Prueba</script><img src=x>").param("categoria", "cuidado")
                .param("disponibilidad", "sin-dato").param("precio", "1000"))
            .andExpect(redirectedUrl("/admin/productos"));

        mvc.perform(get("/js/catalogo.js")).andExpect(content().string(not(containsString("</script>"))));
    }

    @Test
    void lasRutasRespetanLosPermisos() throws Exception {
        mvc.perform(get("/admin/productos")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/productos").with(user("caja@prueba.test"))).andExpect(status().isOk());
        // El cajero consulta, pero no crea ni edita
        mvc.perform(get("/admin/productos/nuevo").with(user("caja@prueba.test"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/productos/1").with(user("caja@prueba.test"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/productos/1/visibilidad").with(csrf()).with(user("caja@prueba.test")).param("activo", "false"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/categorias").with(user("caja@prueba.test"))).andExpect(status().isForbidden());
        // El regente modifica, pero no crea ni retira
        mvc.perform(get("/admin/productos/1").with(user("regente@prueba.test"))).andExpect(status().isOk());
        mvc.perform(get("/admin/productos/nuevo").with(user("regente@prueba.test"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/productos/1/visibilidad").with(csrf()).with(user("regente@prueba.test")).param("activo", "false"))
            .andExpect(status().isForbidden());
        // Sin token CSRF se rechaza incluso al propietario
        mvc.perform(post("/admin/productos/1/visibilidad").with(user("dueno@prueba.test")).param("activo", "false"))
            .andExpect(status().isForbidden());
    }

    @Test
    void elFormularioExplicaLosErrores() throws Exception {
        mvc.perform(multipart("/admin/productos/nuevo").with(csrf()).with(user("dueno@prueba.test"))
                .param("tipo", "medicamento").param("nombre", "").param("disponibilidad", "sin-dato"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Escribe el nombre")))
            .andExpect(content().string(containsString("Agrega al menos una presentación")));
    }

    @Test
    void creaEditaYRetiraUnMedicamento() throws Exception {
        int antes = catalogo.visibles(CatalogoProductos.MEDICAMENTO).size();
        mvc.perform(multipart("/admin/productos/nuevo").with(csrf()).with(user("dueno@prueba.test"))
                .param("tipo", "medicamento").param("nombre", "Producto de prueba").param("categoria", "otros")
                .param("disponibilidad", "disponible").param("formula", "true")
                .param("presId", "").param("presTipo", "Caja x10").param("presConc", "5mg").param("presMarca", "Genérico").param("presPrecio", "$ 4.500"))
            .andExpect(redirectedUrl("/admin/productos"));
        var creado = catalogo.visibles(CatalogoProductos.MEDICAMENTO).stream().filter(p -> p.nombre().equals("Producto de prueba")).findFirst().orElseThrow();
        assertEquals(antes + 1, catalogo.visibles(CatalogoProductos.MEDICAMENTO).size());
        assertEquals(4500, creado.precioDesde());
        assertTrue(creado.formula());

        mvc.perform(post("/admin/productos/" + creado.id() + "/visibilidad").with(csrf()).with(user("dueno@prueba.test")).param("activo", "false"))
            .andExpect(redirectedUrl("/admin/productos"));
        assertEquals(antes, catalogo.visibles(CatalogoProductos.MEDICAMENTO).size());
        mvc.perform(get("/js/catalogo.js")).andExpect(content().string(not(containsString("Producto de prueba"))));
    }

    @Test
    void soloAceptaFotosRealesPorSuContenido() throws Exception {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        mvc.perform(multipart("/admin/productos/1").file(new MockMultipartFile("foto", "falsa.png", "image/png", "<svg onload=x>".getBytes()))
                .with(csrf()).with(user("dueno@prueba.test")).param("tipo", "medicamento").param("nombre", "Acetaminofén")
                .param("categoria", "analgesicos").param("disponibilidad", "sin-dato")
                .param("presTipo", "Unidad").param("presConc", "500mg").param("presMarca", "Genérico").param("presPrecio", "150"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("La foto debe ser JPG, PNG o WEBP")));

        mvc.perform(multipart("/admin/productos/2").file(new MockMultipartFile("foto", "../../evil.png", "image/png", png))
                .with(csrf()).with(user("dueno@prueba.test")).param("tipo", "medicamento").param("nombre", "Ibuprofeno")
                .param("categoria", "antiinflamatorios").param("disponibilidad", "sin-dato")
                .param("presTipo", "Unidad").param("presConc", "200mg").param("presMarca", "Genérico").param("presPrecio", "200"))
            .andExpect(redirectedUrl("/admin/productos"));
        String foto = catalogo.buscar(2).orElseThrow().foto();
        assertNotEquals("", foto);
        assertFalse(foto.contains("evil"));
        mvc.perform(get("/productos/imagen/" + foto)).andExpect(status().isOk());
        mvc.perform(get("/productos/imagen/..%2F..%2Fpom.xml")).andExpect(status().is4xxClientError());
    }

    @Test
    void laCategoriaConProductosNoSeElimina() throws Exception {
        assertTrue(catalogo.eliminarCategoria("analgesicos") != null);
        mvc.perform(post("/admin/categorias").with(csrf()).with(user("dueno@prueba.test")).param("nombre", "Bebés").param("tipo", "variedad"))
            .andExpect(redirectedUrl("/admin/categorias"));
        assertTrue(catalogo.categorias().stream().anyMatch(c -> c.id().equals("bebes")));
        assertEquals(null, catalogo.eliminarCategoria("bebes"));
    }
}
