package com.msequipamento.equipamento.dto;

import com.msequipamento.equipamento.CondicaoEquipamento;
import jakarta.validation.constraints.NotNull;

public record CondicaoRequest(
		@NotNull(message = "Condição é obrigatória")
		CondicaoEquipamento condicao
) {
}
