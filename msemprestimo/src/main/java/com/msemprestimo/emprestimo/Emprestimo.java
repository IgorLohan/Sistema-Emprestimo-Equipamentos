package com.msemprestimo.emprestimo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_emprestimos")
@Getter
@Setter
@NoArgsConstructor
public class Emprestimo {

	@Id
	private UUID id;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "equipamento_id", nullable = false)
	private UUID equipamentoId;

	@Column(name = "data_emprestimo", nullable = false)
	private LocalDateTime dataEmprestimo;

	@Column(name = "data_devolucao")
	private LocalDateTime dataDevolucao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusEmprestimo status;

}
