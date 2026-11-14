package com.msusuario.usuario;

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
@Table(name = "tb_usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, unique = true, length = 160)
	private String email;

	@Column(nullable = false, unique = true, length = 40)
	private String matricula;

}
