package com.botica.inteligente.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/** Ejecuta trabajo fuera del hilo de JavaFX y devuelve el resultado en el hilo de JavaFX. */
public final class Fondo {

    private Fondo() {
    }

    public static <T> void ejecutar(Callable<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(evento -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(evento -> alFallar.accept(tarea.getException()));
        Thread hilo = new Thread(tarea, "botica-fondo");
        hilo.setDaemon(true);
        hilo.start();
    }
}
