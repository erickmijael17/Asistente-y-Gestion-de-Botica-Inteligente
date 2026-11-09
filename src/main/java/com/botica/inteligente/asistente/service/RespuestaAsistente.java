package com.botica.inteligente.asistente.service;

import com.botica.inteligente.shared.i18n.Mensajes;
import java.util.List;

public record RespuestaAsistente(String texto, List<Sugerencia> sugerencias, String aviso) {

    public static String avisoOrientativo() {
        return Mensajes.get("asistente.avisoOrientativo");
    }
}
