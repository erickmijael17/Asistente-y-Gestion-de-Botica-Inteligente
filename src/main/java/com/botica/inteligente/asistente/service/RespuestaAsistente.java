package com.botica.inteligente.asistente.service;

import java.util.List;

public record RespuestaAsistente(String texto, List<Sugerencia> sugerencias, String aviso) {

    public static final String AVISO_ORIENTATIVO =
        "Información orientativa basada en el catálogo de la botica. No sustituye el diagnóstico ni la "
            + "prescripción de un profesional de salud.";
}
