package com.botica.inteligente.usuario.service;

import com.botica.inteligente.usuario.enums.Rol;

public record UsuarioResumen(Long id, String username, Rol rol, boolean estado) {
}
