package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.configuracion.PerfilAdmin;
import com.example.DrogueriaLosTresHermanos.modelo.PlanSuscripcion;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

/**
 * Controlador principal de vistas (capa Controlador del patrón MVC).
 *
 * Responsabilidad: recibir peticiones HTTP, preparar el modelo de datos
 * y delegar la renderización a las plantillas Thymeleaf (capa Vista).
 *
 * ⚠️ PREPARADO PARA BD: cuando se integre Spring Data JPA, inyectar
 *    los servicios (ClienteServicio, ProductoServicio, etc.) con @Autowired
 *    y reemplazar los datos de ejemplo por consultas reales.
 *
 * Rutas mapeadas (12 páginas públicas):
 *   /              → index.html
 *   /medicamentos  → medicamentos.html
 *   /variedades    → variedades.html
 *   /servicios     → servicios.html
 *   /suscripciones → suscripciones.html
 *   /nosotros      → nosotros.html
 *   /contacto      → contacto.html
 *   /registro      → registro.html
 *   /login         → login.html
 *   /perfil        → perfil.html
 *   /checkout      → checkout.html
 *   /recibo        → recibo.html
 */
@Controller
public class PaginaControlador {

    // ── Datos de los 4 planes (en producción vendrán de BD) ──────────────────
    private List<PlanSuscripcion> obtenerPlanes() {
        return Arrays.asList(
            new PlanSuscripcion(1L, "Básico",
                "Acceso esencial al catálogo y pedidos por WhatsApp.",
                4900, 3, false, false, true, false),

            new PlanSuscripcion(2L, "Estándar",
                "Ahorra en tus compras del mes con un buen descuento.",
                9900, 5, false, false, true, false),

            new PlanSuscripcion(3L, "Regular",
                "Domicilio prioritario y descuento ampliado para clientes frecuentes.",
                14900, 10, true, false, true, false),

            new PlanSuscripcion(4L, "Premium",
                "Máximo ahorro, domicilio prioritario y acceso exclusivo a ofertas.",
                19900, 15, true, true, true, true)
        );
    }

    // ── Página de inicio ─────────────────────────────────────────────────────
    @GetMapping("/")
    public String inicio(Model modelo) {
        modelo.addAttribute("pagina", "inicio");
        modelo.addAttribute("titulo", "Inicio");
        return "index";
    }

    // ── Catálogo de medicamentos ─────────────────────────────────────────────
    @GetMapping("/medicamentos")
    public String medicamentos(Model modelo) {
        modelo.addAttribute("pagina", "medicamentos");
        modelo.addAttribute("titulo", "Medicamentos");
        return "medicamentos";
    }

    // ── Catálogo de variedades ───────────────────────────────────────────────
    @GetMapping("/variedades")
    public String variedades(Model modelo) {
        modelo.addAttribute("pagina", "variedades");
        modelo.addAttribute("titulo", "Variedades");
        return "variedades";
    }

    // ── Servicios de la droguería ────────────────────────────────────────────
    @GetMapping("/servicios")
    public String servicios(Model modelo) {
        modelo.addAttribute("pagina", "servicios");
        modelo.addAttribute("titulo", "Servicios");
        return "servicios";
    }

    // ── Planes de suscripción ────────────────────────────────────────────────
    @GetMapping("/suscripciones")
    public String suscripciones(Model modelo) {
        modelo.addAttribute("pagina", "suscripciones");
        modelo.addAttribute("titulo", "Suscripciones");
        modelo.addAttribute("planes", obtenerPlanes());
        return "suscripciones";
    }

    // ── Quiénes somos ────────────────────────────────────────────────────────
    @GetMapping("/nosotros")
    public String nosotros(Model modelo) {
        modelo.addAttribute("pagina", "nosotros");
        modelo.addAttribute("titulo", "Nosotros");
        return "nosotros";
    }

    // ── Contacto ─────────────────────────────────────────────────────────────
    @GetMapping("/contacto")
    public String contacto(Model modelo) {
        modelo.addAttribute("pagina", "contacto");
        modelo.addAttribute("titulo", "Contacto");
        return "contacto";
    }

    // ── Registro de clientes ─────────────────────────────────────────────────
    @GetMapping("/registro")
    public String registro(Model modelo) {
        modelo.addAttribute("pagina", "registro");
        modelo.addAttribute("titulo", "Registro");
        return "registro";
    }

    // ── Login de clientes ────────────────────────────────────────────────────
    @GetMapping("/login")
    public String login(Model modelo) {
        modelo.addAttribute("pagina", "login");
        modelo.addAttribute("titulo", "Iniciar sesión");
        return "login";
    }

    // ── Perfil del cliente ───────────────────────────────────────────────────
    @GetMapping("/perfil")
    public String perfil(Model modelo) {
        modelo.addAttribute("pagina", "perfil");
        modelo.addAttribute("titulo", "Mi perfil");
        return "perfil";
    }

    // ── Proceso de checkout ──────────────────────────────────────────────────
    @GetMapping("/checkout")
    public String checkout(Model modelo) {
        modelo.addAttribute("pagina", "checkout");
        modelo.addAttribute("titulo", "Finalizar pedido");
        return "checkout";
    }

    // ── Recibo / confirmación de pedido ──────────────────────────────────────
    @GetMapping("/recibo")
    public String recibo(Model modelo) {
        modelo.addAttribute("pagina", "recibo");
        modelo.addAttribute("titulo", "Confirmación de pedido");
        return "recibo";
    }

    // ── Pago de suscripción ───────────────────────────────────────────────────
    @GetMapping("/pago-suscripcion")
    public String pagoSuscripcion(Model modelo) {
        modelo.addAttribute("pagina", "pago-suscripcion");
        modelo.addAttribute("titulo", "Pagar Suscripción");
        return "pago-suscripcion";
    }

    // ── Panel de administración ───────────────────────────────────────────────
    @GetMapping("/admin")
    public String adminPanel(Model modelo, Authentication autenticacion) {
        PerfilAdmin perfil = PerfilAdmin.de(autenticacion);
        modelo.addAttribute("pagina", "admin");
        modelo.addAttribute("titulo", "Panel de Administración");
        modelo.addAttribute("adminCorreo", perfil.correo());
        modelo.addAttribute("perfil", perfil);
        modelo.addAttribute("seccionActiva", "resumen");
        return "admin";
    }

    @GetMapping("/admin/login")
    public String adminLogin(Model modelo) {
        modelo.addAttribute("pagina", "admin-login");
        modelo.addAttribute("titulo", "Acceso Administrador");
        return "admin-login";
    }
}
