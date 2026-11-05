package com.botica.inteligente.asistente.service;

import com.botica.inteligente.inventario.repository.InventarioConsultaRepository;
import com.botica.inteligente.inventario.service.InventarioFila;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.producto.service.ProductoFiltro;
import com.botica.inteligente.producto.specification.ProductoSpecification;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.util.Textos;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asistente de consulta sobre el catalogo: entiende nombres, principios activos y sintomas
 * comunes, y propone productos disponibles en el inventario. Funciona localmente, sin servicios
 * externos, y solo usa informacion registrada en el catalogo.
 */
@Service
public class AsistenteService {

    private static final int MAXIMO_SUGERENCIAS = 6;
    private static final int PUNTOS_NOMBRE = 6;
    private static final int PUNTOS_INDICACION = 4;
    private static final int PUNTOS_CATEGORIA = 2;
    private static final int BONO_CON_STOCK = 3;

    private static final Set<String> PALABRAS_VACIAS = Set.of(
        "para", "que", "una", "uno", "unos", "unas", "los", "las", "del", "con", "sin", "por", "como", "mas",
        "mis", "tengo", "tiene", "tienen", "quiero", "necesito", "algo", "pastilla", "pastillas", "tableta",
        "tabletas", "capsula", "capsulas", "jarabe", "medicamento", "medicamentos", "medicina", "medicinas",
        "remedio", "remedios", "hay", "sirve", "sirven", "cual", "cuales", "son", "esta", "este", "puedo",
        "tomar", "dame", "dime", "busco", "favor", "pero", "muy", "cuando", "donde", "usa", "usar", "info",
        "informacion", "sobre", "acerca", "tipo"
    );

    private static final Set<String> PALABRAS_ALTERNATIVA = Set.of(
        "alternativa", "alternativas", "reemplazo", "reemplazos", "sustituto", "sustitutos", "similar",
        "similares", "equivalente", "equivalentes", "otra", "otras", "otro", "otros", "generico", "genericos"
    );

    private static final Set<String> PALABRAS_FICHA = Set.of(
        "sirve", "indicaciones", "indicacion", "contraindicaciones", "contraindicacion", "advertencias",
        "efectos", "dosis", "usa", "usos", "uso"
    );

    /** Sintomas y expresiones comunes -> terminos que suelen aparecer en las fichas del catalogo. */
    private static final Map<String, List<String>> SINONIMOS = Map.ofEntries(
        Map.entry("fiebre", List.of("fiebre", "febril", "antipiretico", "temperatura")),
        Map.entry("calentura", List.of("fiebre", "febril", "antipiretico", "temperatura")),
        Map.entry("febril", List.of("fiebre", "febril", "antipiretico")),
        Map.entry("dolor", List.of("dolor", "analgesico")),
        Map.entry("duele", List.of("dolor", "analgesico")),
        Map.entry("cabeza", List.of("cefalea", "migrana", "cabeza")),
        Map.entry("cefalea", List.of("cefalea", "migrana", "cabeza")),
        Map.entry("migrana", List.of("cefalea", "migrana")),
        Map.entry("garganta", List.of("garganta", "faringitis", "amigdalitis")),
        Map.entry("tos", List.of("tos", "antitusivo", "expectorante")),
        Map.entry("gripe", List.of("gripe", "resfriado", "resfrio", "congestion")),
        Map.entry("resfrio", List.of("gripe", "resfriado", "resfrio", "congestion")),
        Map.entry("resfriado", List.of("gripe", "resfriado", "resfrio", "congestion")),
        Map.entry("catarro", List.of("gripe", "resfriado", "congestion")),
        Map.entry("alergia", List.of("alergia", "alergico", "antihistaminico", "rinitis")),
        Map.entry("alergico", List.of("alergia", "alergico", "antihistaminico", "rinitis")),
        Map.entry("picazon", List.of("alergia", "prurito", "antihistaminico")),
        Map.entry("estornudos", List.of("alergia", "rinitis", "antihistaminico")),
        Map.entry("estomago", List.of("estomago", "acidez", "gastritis", "antiacido")),
        Map.entry("acidez", List.of("acidez", "gastritis", "antiacido", "reflujo")),
        Map.entry("gastritis", List.of("gastritis", "acidez", "antiacido")),
        Map.entry("reflujo", List.of("reflujo", "acidez", "antiacido")),
        Map.entry("diarrea", List.of("diarrea", "antidiarreico")),
        Map.entry("nauseas", List.of("nauseas", "vomito", "antiemetico")),
        Map.entry("vomito", List.of("nauseas", "vomito", "antiemetico")),
        Map.entry("inflamacion", List.of("inflamacion", "antiinflamatorio")),
        Map.entry("inflamado", List.of("inflamacion", "antiinflamatorio")),
        Map.entry("hinchazon", List.of("inflamacion", "antiinflamatorio")),
        Map.entry("infeccion", List.of("infeccion", "antibiotico", "bacteriana")),
        Map.entry("insomnio", List.of("insomnio", "sueno", "sedante")),
        Map.entry("colico", List.of("colico", "espasmo", "antiespasmodico")),
        Map.entry("colicos", List.of("colico", "espasmo", "antiespasmodico")),
        Map.entry("retortijones", List.of("colico", "espasmo", "antiespasmodico")),
        Map.entry("muscular", List.of("muscular", "contractura", "relajante")),
        Map.entry("contractura", List.of("muscular", "contractura", "relajante")),
        Map.entry("espalda", List.of("muscular", "lumbalgia", "dolor")),
        Map.entry("estrenimiento", List.of("estrenimiento", "laxante")),
        Map.entry("hongos", List.of("hongos", "antimicotico", "micosis")),
        Map.entry("vitamina", List.of("vitamina", "suplemento")),
        Map.entry("vitaminas", List.of("vitamina", "suplemento"))
    );

    private final ProductoRepository productoRepository;
    private final InventarioConsultaRepository inventarioRepository;
    private final SesionActual sesionActual;

    public AsistenteService(
        ProductoRepository productoRepository,
        InventarioConsultaRepository inventarioRepository,
        SesionActual sesionActual
    ) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.sesionActual = sesionActual;
    }

    @Transactional(readOnly = true)
    public RespuestaAsistente consultar(String pregunta) {
        sesionActual.exigirAutenticado();
        String limpia = Textos.limpiar(pregunta);
        if (limpia == null) {
            return respuesta("Escriba una consulta: un síntoma, el nombre de un medicamento o un principio activo.", List.of());
        }
        Consulta consulta = Consulta.de(limpia);
        if (consulta.directos().isEmpty()) {
            return respuesta(
                "No entendí la consulta. Pruebe con el nombre de un medicamento, un principio activo o un síntoma, "
                    + "por ejemplo «pastillas para la fiebre».", List.of()
            );
        }

        Map<Long, Long> stock = inventarioRepository.filas("").stream()
            .collect(Collectors.toMap(InventarioFila::productoId, InventarioFila::stock));
        List<Candidato> candidatos = productoRepository
            .findAll(ProductoSpecification.filtrar(new ProductoFiltro(null, true, null, null, null)))
            .stream()
            .map(producto -> puntuar(producto, consulta, stock.getOrDefault(producto.getId(), 0L)))
            .filter(candidato -> candidato.puntaje() > 0)
            .toList();
        if (candidatos.isEmpty()) {
            return respuesta(
                "No encontré productos en el catálogo relacionados con «" + limpia + "». Verifique el nombre o pruebe "
                    + "con otro síntoma o principio activo.", List.of()
            );
        }

        Candidato mejor = candidatos.stream().max(Comparator.comparingInt(Candidato::puntajeNombre)
            .thenComparingInt(Candidato::puntaje)).orElseThrow();
        boolean preguntaPorProducto = mejor.puntajeNombre() > 0;

        if (consulta.pideAlternativas() && preguntaPorProducto) {
            return alternativas(mejor, candidatos, stock);
        }
        if (consulta.pideFicha() && preguntaPorProducto) {
            return ficha(mejor);
        }
        return opciones(limpia, candidatos);
    }

    // ---------- respuestas ----------

    private RespuestaAsistente alternativas(Candidato base, List<Candidato> candidatos, Map<Long, Long> stock) {
        Producto producto = base.producto();
        String principio = normalizar(producto.getPrincipioActivo());
        List<Candidato> mismas = new ArrayList<>();
        if (!principio.isEmpty()) {
            for (Producto otro : productoRepository
                .findAll(ProductoSpecification.filtrar(new ProductoFiltro(null, true, null, null, null)))) {
                if (!otro.getId().equals(producto.getId()) && normalizar(otro.getPrincipioActivo()).equals(principio)) {
                    mismas.add(new Candidato(otro, 1, 0, stock.getOrDefault(otro.getId(), 0L), List.of()));
                }
            }
        }
        List<Candidato> lista = mismas.isEmpty()
            ? candidatos.stream().filter(c -> !c.producto().getId().equals(producto.getId())).toList()
            : mismas;
        List<Sugerencia> sugerencias = ordenar(lista).stream().limit(MAXIMO_SUGERENCIAS).map(this::sugerencia).toList();
        if (sugerencias.isEmpty()) {
            return respuesta("No hay otros productos en el catálogo que sirvan como alternativa a " + producto.getNombreComercial() + ".", List.of());
        }
        String criterio = mismas.isEmpty()
            ? "con indicaciones relacionadas"
            : "con el mismo principio activo (" + producto.getPrincipioActivo() + ")";
        return respuesta(
            "Alternativas a " + producto.getNombreComercial() + " " + criterio + ": " + sugerencias.size()
                + " opción(es), " + conStock(sugerencias) + ".", sugerencias
        );
    }

    private RespuestaAsistente ficha(Candidato candidato) {
        Sugerencia ficha = sugerencia(candidato);
        StringBuilder texto = new StringBuilder(ficha.nombre());
        if (ficha.principioActivo() != null) {
            texto.append(" (").append(ficha.principioActivo());
            if (ficha.concentracion() != null) {
                texto.append(' ').append(ficha.concentracion());
            }
            texto.append(')');
        }
        texto.append(": ");
        texto.append(ficha.indicaciones() != null
            ? "indicado para " + ficha.indicaciones() + "."
            : "el catálogo no tiene indicaciones registradas para este producto.");
        if (ficha.requiereReceta()) {
            texto.append(" Requiere receta médica.");
        }
        return respuesta(texto.toString(), List.of(ficha));
    }

    private RespuestaAsistente opciones(String pregunta, List<Candidato> candidatos) {
        List<Sugerencia> sugerencias = diversificar(ordenar(candidatos));
        return respuesta(
            "Para «" + pregunta + "» pueden servir " + sugerencias.size() + " opción(es) del inventario, "
                + conStock(sugerencias) + ". Son sugerencias con distinto principio activo: confirme con el paciente "
                + "(alergias, embarazo, otros medicamentos) antes de entregar.",
            sugerencias
        );
    }

    private String conStock(List<Sugerencia> sugerencias) {
        long disponibles = sugerencias.stream().filter(Sugerencia::hayStock).count();
        return disponibles == sugerencias.size() ? "todas con stock" : disponibles + " con stock disponible";
    }

    private RespuestaAsistente respuesta(String texto, List<Sugerencia> sugerencias) {
        return new RespuestaAsistente(texto, sugerencias, RespuestaAsistente.AVISO_ORIENTATIVO);
    }

    // ---------- puntuacion y orden ----------

    private record Candidato(Producto producto, int puntaje, int puntajeNombre, long stock, List<String> cubre) {
    }

    private Candidato puntuar(Producto producto, Consulta consulta, long stock) {
        String nombres = normalizar(producto.getNombreComercial() + " " + producto.getNombreGenerico() + " " + producto.getPrincipioActivo());
        String clinico = normalizar(producto.getIndicacionesOficiales() + " " + producto.getDescripcion());
        String categoria = normalizar(producto.getCategoria().getNombre());
        int nombre = 0;
        int total = 0;
        for (String termino : consulta.directos()) {
            if (nombres.contains(termino)) {
                nombre += PUNTOS_NOMBRE;
            }
            if (clinico.contains(termino)) {
                total += PUNTOS_INDICACION;
            }
            if (categoria.contains(termino)) {
                total += PUNTOS_CATEGORIA;
            }
        }
        for (String termino : consulta.expandidos()) {
            if (nombres.contains(termino)) {
                total += PUNTOS_NOMBRE / 2;
            }
            if (clinico.contains(termino)) {
                total += PUNTOS_INDICACION / 2;
            }
            if (categoria.contains(termino)) {
                total += 1;
            }
        }
        List<String> cubre = new ArrayList<>();
        for (String palabra : consulta.directos()) {
            Set<String> relacionados = new LinkedHashSet<>(consulta.expansiones().getOrDefault(palabra, Set.of()));
            relacionados.add(palabra);
            if (relacionados.stream().anyMatch(t -> nombres.contains(t) || clinico.contains(t) || categoria.contains(t))) {
                cubre.add(palabra);
            }
        }
        return new Candidato(producto, total + nombre, nombre, stock, cubre);
    }

    /** Primero los que tienen stock y mejor puntaje; a igualdad, el mas barato. */
    private List<Candidato> ordenar(List<Candidato> candidatos) {
        return candidatos.stream()
            .sorted(Comparator
                .comparingInt((Candidato c) -> c.puntaje() + (c.stock() > 0 ? BONO_CON_STOCK : 0)).reversed()
                .thenComparing(c -> c.producto().getPrecioVenta()))
            .toList();
    }

    /** Elige un producto por principio activo antes de repetir, para ofrecer alternativas variadas. */
    private List<Sugerencia> diversificar(List<Candidato> ordenados) {
        List<Sugerencia> elegidos = new ArrayList<>();
        Set<String> principios = new HashSet<>();
        List<Candidato> restantes = new ArrayList<>();
        for (Candidato candidato : ordenados) {
            String principio = normalizar(candidato.producto().getPrincipioActivo());
            if (elegidos.size() < MAXIMO_SUGERENCIAS && (principio.isEmpty() || principios.add(principio))) {
                elegidos.add(sugerencia(candidato));
            } else {
                restantes.add(candidato);
            }
        }
        for (Candidato candidato : restantes) {
            if (elegidos.size() >= MAXIMO_SUGERENCIAS) {
                break;
            }
            elegidos.add(sugerencia(candidato));
        }
        return elegidos;
    }

    private Sugerencia sugerencia(Candidato c) {
        Producto p = c.producto();
        return new Sugerencia(
            p.getId(), p.getNombreComercial(), p.getNombreGenerico(), p.getPrincipioActivo(), p.getConcentracion(),
            p.getPresentacion(), p.getLaboratorio().getNombre(), p.getCategoria().getNombre(), p.getPrecioVenta(),
            c.stock(), Boolean.TRUE.equals(p.getRequiereReceta()), p.getIndicacionesOficiales(),
            p.getContraindicaciones(), p.getAdvertencias(), p.getPrecauciones(), p.getViaAdministracion(),
            p.getCondicionesAlmacenamiento(), p.getFuenteInformacion(), p.getFechaActualizacionInformacion(), c.puntaje(),
            c.cubre()
        );
    }

    // ---------- interpretacion de la consulta ----------

    private record Consulta(
        Set<String> directos, Set<String> expandidos, Map<String, Set<String>> expansiones,
        boolean pideAlternativas, boolean pideFicha
    ) {

        static Consulta de(String texto) {
            List<String> palabras = java.util.Arrays.stream(normalizar(texto).split("[^a-z0-9]+"))
                .filter(palabra -> !palabra.isBlank())
                .toList();
            boolean alternativas = palabras.stream().anyMatch(PALABRAS_ALTERNATIVA::contains);
            boolean ficha = palabras.stream().anyMatch(PALABRAS_FICHA::contains);
            Set<String> directos = new LinkedHashSet<>();
            Set<String> expandidos = new LinkedHashSet<>();
            Map<String, Set<String>> expansiones = new java.util.LinkedHashMap<>();
            for (String palabra : palabras) {
                if (palabra.length() < 3 || PALABRAS_VACIAS.contains(palabra) || PALABRAS_ALTERNATIVA.contains(palabra)
                    || PALABRAS_FICHA.contains(palabra)) {
                    continue;
                }
                directos.add(palabra);
                expandidos.addAll(SINONIMOS.getOrDefault(palabra, List.of()));
                expansiones.put(palabra, new LinkedHashSet<>(SINONIMOS.getOrDefault(palabra, List.of())));
            }
            expandidos.removeAll(directos);
            return new Consulta(directos, expandidos, expansiones, alternativas, ficha);
        }
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .trim();
    }
}
