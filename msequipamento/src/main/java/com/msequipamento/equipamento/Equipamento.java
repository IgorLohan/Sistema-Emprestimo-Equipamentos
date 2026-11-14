package com.msequipamento.equipamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tb_equipamentos")
@Getter
@Setter
@NoArgsConstructor
public class Equipamento {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, length = 40)
	private String tipo;

	@Column(nullable = false, unique = true, length = 60)
	private String patrimonio;

	@Column(nullable = false)
	private boolean disponivel = true;

	@Column(name = "reserva_emprestimo_id")
	private UUID reservaEmprestimoId;

}
