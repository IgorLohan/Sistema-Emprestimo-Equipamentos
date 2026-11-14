package com.msusuario.usuario.dto;

import java.util.UUID;

public record UsuarioResponse(
		UUID id,
		String nome,
		String email,
		String matricula
) {
}
