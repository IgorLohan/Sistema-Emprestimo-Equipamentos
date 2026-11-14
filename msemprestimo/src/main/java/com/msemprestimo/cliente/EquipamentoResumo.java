package com.msemprestimo.cliente;

import java.util.UUID;

public record EquipamentoResumo(
		UUID id,
		String nome,
		String tipo,
		String patrimonio,
		boolean disponivel,
		UUID reservaEmprestimoId,
		CondicaoEquipamento condicao
) {
}
