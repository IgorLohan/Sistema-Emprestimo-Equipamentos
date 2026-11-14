package com.msemprestimo.cliente;

import java.util.UUID;

public record UsuarioResumo(
		UUID id,
		String nome,
		String email,
		String matricula
) {
}
