package com.msemprestimo.emprestimo.dto;

import com.msemprestimo.emprestimo.StatusEmprestimo;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmprestimoResponse(
		UUID id,
		UUID usuarioId,
		UUID equipamentoId,
		LocalDateTime dataEmprestimo,
		LocalDateTime dataDevolucao,
		StatusEmprestimo status
) {
}
