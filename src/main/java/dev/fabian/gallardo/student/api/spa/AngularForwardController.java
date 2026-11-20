package dev.fabian.gallardo.student.api.spa;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sirve el index.html del SPA para sus rutas del lado del cliente (frontend/src/app/app.routes.ts), que el
 * navegador pide directo al recargar. Lista explícita, no un comodín: un comodín taparía los 404 de la API.
 * Si agregás una ruta al router de Angular, agregala acá. No uses /error: es la ruta interna de Spring para sus errores.
 */
@Controller
public class AngularForwardController {

    @GetMapping({"/login", "/register", "/dashboard", "/dashboard/**", "/items", "/forbidden", "/server-error"})
    String forwardToIndex() {
        return "forward:/index.html";
    }
}
