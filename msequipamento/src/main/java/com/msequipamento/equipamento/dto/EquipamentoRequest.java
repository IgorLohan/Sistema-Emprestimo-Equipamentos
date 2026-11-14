package com.msequipamento.equipamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipamentoRequest(
		@NotBlank(message = "Nome é obrigatório")
		@Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
		String nome,

		@NotBlank(message = "Tipo é obrigatório")
		@Size(max = 40, message = "Tipo deve ter no máximo 40 caracteres")
		String tipo,

		@NotBlank(message = "Patrimônio é obrigatório")
		@Size(max = 60, message = "Patrimônio deve ter no máximo 60 caracteres")
		String patrimonio
) {
}
