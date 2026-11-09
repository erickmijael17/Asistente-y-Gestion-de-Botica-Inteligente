package com.botica.inteligente.producto.informacion;

/** Ficha extraida heuristicamente de una pagina web; cualquier campo puede venir nulo. */
public record FichaWeb(
    String principioActivo,
    String indicacionesOficiales,
    String contraindicaciones,
    String advertencias,
    String precauciones,
    String url
) {

    public boolean vacia() {
        return principioActivo == null && indicacionesOficiales == null
            && contraindicaciones == null && advertencias == null && precauciones == null;
    }
}
