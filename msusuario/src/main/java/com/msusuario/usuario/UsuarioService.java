package com.msusuario.usuario;

import com.msusuario.excecao.ConflitoException;
import com.msusuario.excecao.RecursoNaoEncontradoException;
import com.msusuario.usuario.dto.UsuarioRequest;
import com.msusuario.usuario.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

	private final UsuarioRepository repository;

	@Transactional
	public UsuarioResponse criar(UsuarioRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String matricula = request.matricula().trim();

		if (repository.existsByEmail(email)) {
			throw new ConflitoException("E-mail já cadastrado");
		}
		if (repository.existsByMatricula(matricula)) {
			throw new ConflitoException("Matrícula já cadastrada");
		}

		Usuario usuario = new Usuario();
		usuario.setNome(request.nome().trim());
		usuario.setEmail(email);
		usuario.setMatricula(matricula);

		try {
			return toResponse(repository.saveAndFlush(usuario));
		} catch (DataIntegrityViolationException ex) {
			throw new ConflitoException(mensagemDuplicidade(ex));
		}
	}

	@Transactional(readOnly = true)
	public UsuarioResponse buscarPorId(UUID id) {
		return repository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

	private UsuarioResponse toResponse(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getMatricula());
	}

	private String mensagemDuplicidade(DataIntegrityViolationException ex) {
		String texto = ex.getMostSpecificCause().getMessage();
		if (texto == null) {
			return "Dados duplicados";
		}
		String normalizado = texto.toLowerCase(Locale.ROOT);
		if (normalizado.contains("email")) {
			return "E-mail já cadastrado";
		}
		if (normalizado.contains("matricula")) {
			return "Matrícula já cadastrada";
		}
		return "Dados duplicados";
	}

}
