package com.botica.inteligente.ui;

import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** Animaciones breves y discretas: aparicion suave de vistas y elevacion al pasar el cursor. */
public final class Animaciones {

    private static final Duration DURACION = Duration.millis(220);
    private static final Duration DURACION_HOVER = Duration.millis(120);
    private static final double DESPLAZAMIENTO = 10;
    private static final double ELEVACION = -2;

    private Animaciones() {
    }

    /** Aparece con un fundido y un ligero desplazamiento hacia arriba. */
    public static void entrada(Node nodo) {
        entrada(nodo, Duration.ZERO);
    }

    /** Aparece uno tras otro con una pequena pausa entre cada nodo. */
    public static void escalonada(List<? extends Node> nodos) {
        for (int i = 0; i < nodos.size(); i++) {
            entrada(nodos.get(i), Duration.millis(50L * i));
        }
    }

    /** Sube un par de pixeles mientras el cursor esta encima. */
    public static void elevarAlPasar(Node nodo) {
        nodo.setOnMouseEntered(evento -> mover(nodo, ELEVACION));
        nodo.setOnMouseExited(evento -> mover(nodo, 0));
    }

    private static void entrada(Node nodo, Duration retraso) {
        if (nodo == null) {
            return;
        }
        nodo.setOpacity(0);
        nodo.setTranslateY(DESPLAZAMIENTO);
        FadeTransition fundido = new FadeTransition(DURACION, nodo);
        fundido.setToValue(1);
        TranslateTransition subida = new TranslateTransition(DURACION, nodo);
        subida.setToY(0);
        ParallelTransition transicion = new ParallelTransition(fundido, subida);
        transicion.setInterpolator(Interpolator.EASE_OUT);
        transicion.setDelay(retraso);
        transicion.play();
    }

    private static void mover(Node nodo, double destino) {
        TranslateTransition transicion = new TranslateTransition(DURACION_HOVER, nodo);
        transicion.setToY(destino);
        transicion.setInterpolator(Interpolator.EASE_OUT);
        transicion.play();
    }
}
