package com.msequipamento.equipamento.dto;

import java.util.UUID;

public record EquipamentoResponse(
		UUID id,
		String nome,
		String tipo,
		String patrimonio,
		boolean disponivel,
		UUID reservaEmprestimoId
) {
}
