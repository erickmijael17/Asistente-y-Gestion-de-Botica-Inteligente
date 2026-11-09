package com.botica.inteligente.producto.informacion;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Busca y extrae informacion de medicamentos en la web mediante scraping (DuckDuckGo + heuristica
 * por palabras clave). No hay API oficial detras: depende de que esos sitios sigan respondiendo
 * HTML estable, por lo que cualquier fallo se trata como "sin resultados", nunca como error fatal.
 */
@Service
public class BusquedaWebService {

    private static final Logger log = LoggerFactory.getLogger(BusquedaWebService.class);
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final int TIMEOUT_MS = 8000;
    private static final int MAXIMO_RESULTADOS = 5;
    private static final int LONGITUD_MINIMA_BLOQUE = 40;
    private static final int LONGITUD_MAXIMA_CAMPO = 950;
    private static final int LONGITUD_MAXIMA_PRINCIPIO_ACTIVO = 170;

    public List<ResultadoBusquedaWeb> buscar(String consulta) {
        String texto = consulta == null ? "" : consulta.trim();
        if (texto.isEmpty()) {
            return List.of();
        }
        try {
            Document pagina = Jsoup.connect("https://html.duckduckgo.com/html/")
                .data("q", texto)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
            List<ResultadoBusquedaWeb> resultados = new ArrayList<>();
            for (Element enlace : pagina.select("a.result__a")) {
                String url = enlace.absUrl("href");
                if (url.isBlank()) {
                    url = enlace.attr("href");
                }
                String titulo = enlace.text();
                if (!url.isBlank() && !titulo.isBlank()) {
                    resultados.add(new ResultadoBusquedaWeb(titulo, url));
                }
                if (resultados.size() >= MAXIMO_RESULTADOS) {
                    break;
                }
            }
            return resultados;
        } catch (IOException | RuntimeException excepcion) {
            log.warn("No fue posible buscar informacion web para \"{}\"", texto, excepcion);
            return List.of();
        }
    }

    public FichaWeb extraerFicha(String url) {
        try {
            Document pagina = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
            List<String> bloques = new ArrayList<>();
            for (Element bloque : pagina.select("p, li, td")) {
                String texto = bloque.text().trim();
                if (texto.length() >= LONGITUD_MINIMA_BLOQUE) {
                    bloques.add(texto);
                }
            }
            return clasificar(bloques, url);
        } catch (IOException | RuntimeException excepcion) {
            log.warn("No fue posible descargar la ficha de {}", url, excepcion);
            return new FichaWeb(null, null, null, null, null, url);
        }
    }

    /** Clasifica bloques de texto por palabra clave; separado para poder probarlo sin red. */
    FichaWeb clasificar(List<String> bloques, String url) {
        String principioActivo = null;
        String indicaciones = null;
        String contraindicaciones = null;
        String advertencias = null;
        String precauciones = null;
        for (String bloque : bloques) {
            String normalizado = normalizar(bloque);
            if (contraindicaciones == null && normalizado.contains("contraindicac")) {
                contraindicaciones = recortar(bloque, LONGITUD_MAXIMA_CAMPO);
            } else if (indicaciones == null && normalizado.contains("indicac")) {
                indicaciones = recortar(bloque, LONGITUD_MAXIMA_CAMPO);
            } else if (advertencias == null && normalizado.contains("advertenc")) {
                advertencias = recortar(bloque, LONGITUD_MAXIMA_CAMPO);
            } else if (precauciones == null && normalizado.contains("precauc")) {
                precauciones = recortar(bloque, LONGITUD_MAXIMA_CAMPO);
            } else if (principioActivo == null
                && (normalizado.contains("principio activo") || normalizado.contains("composicion"))) {
                principioActivo = recortar(bloque, LONGITUD_MAXIMA_PRINCIPIO_ACTIVO);
            }
        }
        return new FichaWeb(principioActivo, indicaciones, contraindicaciones, advertencias, precauciones, url);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
