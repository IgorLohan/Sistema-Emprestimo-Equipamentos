package com.msemprestimo.emprestimo;

import com.msemprestimo.cliente.EquipamentoCondicaoClient;
import com.msemprestimo.cliente.EquipamentoClient;
import com.msemprestimo.cliente.UsuarioClient;
import com.msemprestimo.emprestimo.dto.DevolucaoRequest;
import com.msemprestimo.emprestimo.dto.EmprestimoRequest;
import com.msemprestimo.emprestimo.dto.EmprestimoResponse;
import com.msemprestimo.excecao.ComunicacaoException;
import com.msemprestimo.excecao.ConflitoException;
import com.msemprestimo.excecao.RecursoNaoEncontradoException;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmprestimoService {

	private final EmprestimoRepository repository;
	private final EmprestimoGravacao gravacao;
	private final UsuarioClient usuarioClient;
	private final EquipamentoClient equipamentoClient;
	private final EquipamentoCondicaoClient equipamentoCondicaoClient;

	public EmprestimoResponse solicitar(EmprestimoRequest request) {
		validarUsuario(request.usuarioId());

		UUID emprestimoId = UUID.randomUUID();
		reservarEquipamento(request.equipamentoId(), emprestimoId);

		Emprestimo emprestimo = new Emprestimo();
		emprestimo.setId(emprestimoId);
		emprestimo.setUsuarioId(request.usuarioId());
		emprestimo.setEquipamentoId(request.equipamentoId());
		emprestimo.setDataEmprestimo(LocalDateTime.now());
		emprestimo.setStatus(StatusEmprestimo.ATIVO);

		try {
			return toResponse(gravacao.salvar(emprestimo));
		} catch (DataIntegrityViolationException ex) {
			compensarReserva(request.equipamentoId(), emprestimoId);
			throw new ConflitoException("Já existe um empréstimo ativo para este equipamento");
		} catch (RuntimeException ex) {
			compensarReserva(request.equipamentoId(), emprestimoId);
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public EmprestimoResponse buscarPorId(UUID id) {
		return repository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado"));
	}

	@Transactional(readOnly = true)
	public List<EmprestimoResponse> listarPorUsuario(UUID usuarioId) {
		return repository.findByUsuarioIdOrderByDataEmprestimoDesc(usuarioId).stream()
				.map(this::toResponse)
				.toList();
	}

	public EmprestimoResponse devolver(UUID id, DevolucaoRequest request) {
		Emprestimo emprestimo = repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado"));

		if (emprestimo.getStatus() == StatusEmprestimo.DEVOLVIDO) {
			return toResponse(emprestimo);
		}
		if (emprestimo.getStatus() != StatusEmprestimo.ATIVO) {
			throw new ConflitoException("Somente empréstimos ativos podem ser devolvidos");
		}

		equipamentoCondicaoClient.alterarCondicao(emprestimo.getEquipamentoId(), request.condicao());
		liberarEquipamento(emprestimo.getEquipamentoId(), emprestimo.getId());

		Emprestimo atualizado = gravacao.marcarDevolvido(id, LocalDateTime.now())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado"));
		if (atualizado.getStatus() != StatusEmprestimo.DEVOLVIDO) {
			throw new ConflitoException("Somente empréstimos ativos podem ser devolvidos");
		}
		return toResponse(atualizado);
	}

	private void validarUsuario(UUID usuarioId) {
		try {
			usuarioClient.buscarPorId(usuarioId);
		} catch (FeignException.NotFound ex) {
			throw new RecursoNaoEncontradoException("Usuário não encontrado");
		} catch (RetryableException ex) {
			throw new ComunicacaoException("Serviço de usuários indisponível", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (FeignException ex) {
			throw new ComunicacaoException("Não foi possível consultar o serviço de usuários", HttpStatus.BAD_GATEWAY);
		}
	}

	private void reservarEquipamento(UUID equipamentoId, UUID emprestimoId) {
		try {
			equipamentoClient.buscarPorId(equipamentoId);
		} catch (FeignException.NotFound ex) {
			throw new RecursoNaoEncontradoException("Equipamento não encontrado");
		} catch (RetryableException ex) {
			throw new ComunicacaoException("Serviço de equipamentos indisponível", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (FeignException ex) {
			throw new ComunicacaoException("Não foi possível consultar o serviço de equipamentos", HttpStatus.BAD_GATEWAY);
		}

		try {
			equipamentoClient.reservar(equipamentoId, emprestimoId);
		} catch (FeignException.NotFound ex) {
			throw new RecursoNaoEncontradoException("Equipamento não encontrado");
		} catch (FeignException.Conflict ex) {
			throw new ConflitoException("Equipamento indisponível");
		} catch (RetryableException ex) {
			throw new ComunicacaoException("Serviço de equipamentos indisponível", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (FeignException ex) {
			throw new ComunicacaoException("Não foi possível reservar o equipamento", HttpStatus.BAD_GATEWAY);
		}
	}

	private void liberarEquipamento(UUID equipamentoId, UUID emprestimoId) {
		try {
			equipamentoClient.liberar(equipamentoId, emprestimoId);
		} catch (FeignException.NotFound ex) {
			throw new RecursoNaoEncontradoException("Equipamento não encontrado");
		} catch (FeignException.Conflict ex) {
			throw new ConflitoException("Não foi possível liberar o equipamento");
		} catch (RetryableException ex) {
			throw new ComunicacaoException("Serviço de equipamentos indisponível", HttpStatus.SERVICE_UNAVAILABLE);
		} catch (FeignException ex) {
			throw new ComunicacaoException("Não foi possível liberar o equipamento", HttpStatus.BAD_GATEWAY);
		}
	}

	private void compensarReserva(UUID equipamentoId, UUID emprestimoId) {
		try {
			equipamentoClient.liberar(equipamentoId, emprestimoId);
		} catch (FeignException ex) {
			log.error("Falha ao compensar a reserva {} do equipamento {}", emprestimoId, equipamentoId, ex);
		}
	}

	private EmprestimoResponse toResponse(Emprestimo emprestimo) {
		return new EmprestimoResponse(
				emprestimo.getId(),
				emprestimo.getUsuarioId(),
				emprestimo.getEquipamentoId(),
				emprestimo.getDataEmprestimo(),
				emprestimo.getDataDevolucao(),
				emprestimo.getStatus()
		);
	}

}
