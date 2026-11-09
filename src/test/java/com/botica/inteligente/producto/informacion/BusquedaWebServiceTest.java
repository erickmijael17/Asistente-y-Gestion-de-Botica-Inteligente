package com.botica.inteligente.producto.informacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BusquedaWebServiceTest {

    private final BusquedaWebService servicio = new BusquedaWebService();

    @Test
    void clasificaCadaBloqueEnElCampoCorrectoSegunPalabraClave() {
        List<String> bloques = List.of(
            "Composición: cada tableta contiene Paracetamol 500 mg como principio activo.",
            "Indicaciones: alivio sintomático del dolor leve a moderado y la fiebre.",
            "Contraindicaciones: hipersensibilidad conocida al paracetamol o a alguno de sus componentes.",
            "Advertencias: no exceder la dosis diaria recomendada sin supervisión médica.",
            "Precauciones: usar con cautela en pacientes con insuficiencia hepática o renal."
        );

        FichaWeb ficha = servicio.clasificar(bloques, "https://ejemplo.com/ficha");

        assertTrue(ficha.principioActivo().contains("Paracetamol"));
        assertTrue(ficha.indicacionesOficiales().contains("alivio sintom"));
        assertTrue(ficha.contraindicaciones().contains("hipersensibilidad"));
        assertTrue(ficha.advertencias().contains("dosis diaria"));
        assertTrue(ficha.precauciones().contains("insuficiencia hep"));
        assertEquals("https://ejemplo.com/ficha", ficha.url());
        assertTrue(!ficha.vacia());
    }

    @Test
    void sinBloquesRelevantesDevuelveFichaVacia() {
        List<String> bloques = List.of(
            "Este producto se almacena en un lugar fresco y seco.",
            "Precio sugerido de venta al público disponible en tiendas autorizadas."
        );

        FichaWeb ficha = servicio.clasificar(bloques, "https://ejemplo.com/otra");

        assertNull(ficha.indicacionesOficiales());
        assertNull(ficha.contraindicaciones());
        assertTrue(ficha.vacia());
    }

    @Test
    void noConfundeContraindicacionesConIndicacionesAunqueApareceAntes() {
        List<String> bloques = List.of(
            "Contraindicaciones: no usar en pacientes con alergia conocida al principio activo.",
            "Indicaciones: tratamiento del dolor leve a moderado y la fiebre."
        );

        FichaWeb ficha = servicio.clasificar(bloques, "https://ejemplo.com/orden");

        assertTrue(ficha.contraindicaciones().contains("alergia"));
        assertTrue(ficha.indicacionesOficiales().contains("dolor leve"));
    }

    @Test
    void ignoraMayusculasYTildesAlClasificar() {
        List<String> bloques = List.of(
            "INDICACIÓN: tratamiento sintomático de la cefalea y el dolor muscular leve a moderado."
        );

        FichaWeb ficha = servicio.clasificar(bloques, "https://ejemplo.com/tilde");

        assertTrue(ficha.indicacionesOficiales() != null);
    }
}
