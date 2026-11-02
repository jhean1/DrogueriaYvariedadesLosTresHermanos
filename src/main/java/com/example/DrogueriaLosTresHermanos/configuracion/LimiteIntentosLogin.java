package com.example.DrogueriaLosTresHermanos.configuracion;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena ataques de fuerza bruta contra /admin/login: bloquea una IP durante
 * 15 minutos después de 5 intentos fallidos consecutivos.
 *
 * Detrás de un proxy inverso, la IP visible es la del proxy; en ese caso hay que
 * configurar server.forward-headers-strategy para que el límite sea por cliente.
 */
@Component
public class LimiteIntentosLogin {

    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Registro(int fallos, Instant expira) {}

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public boolean estaBloqueada(String ip) {
        Registro registro = registros.get(ip);
        if (registro == null) {
            return false;
        }
        if (Instant.now().isAfter(registro.expira())) {
            registros.remove(ip, registro);
            return false;
        }
        return registro.fallos() >= MAX_INTENTOS;
    }

    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        String ip = ipDe(evento.getAuthentication().getDetails());
        if (ip == null) {
            return;
        }
        Instant ahora = Instant.now();
        // Evita que el mapa crezca sin límite con registros ya vencidos.
        registros.values().removeIf(registro -> ahora.isAfter(registro.expira()));
        registros.merge(ip, new Registro(1, ahora.plus(BLOQUEO)),
            (actual, nuevo) -> new Registro(actual.fallos() + 1, nuevo.expira()));
    }

    @EventListener
    public void alEntrar(AuthenticationSuccessEvent evento) {
        String ip = ipDe(evento.getAuthentication().getDetails());
        if (ip != null) {
            registros.remove(ip);
        }
    }

    private static String ipDe(Object detalles) {
        return detalles instanceof WebAuthenticationDetails web ? web.getRemoteAddress() : null;
    }
}
