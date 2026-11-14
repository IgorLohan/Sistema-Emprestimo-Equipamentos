package com.msemprestimo.emprestimo.dto;

import com.msemprestimo.cliente.CondicaoEquipamento;
import jakarta.validation.constraints.NotNull;

public record DevolucaoRequest(
		@NotNull(message = "Condição é obrigatória")
		CondicaoEquipamento condicao
) {
}
