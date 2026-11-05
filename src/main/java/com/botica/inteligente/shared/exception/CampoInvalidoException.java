package com.botica.inteligente.shared.exception;

/** Error de validacion asociado a un campo concreto del formulario. */
public class CampoInvalidoException extends NegocioException {

    private final String campo;

    public CampoInvalidoException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
