package com.msemprestimo.cliente;

import com.msemprestimo.excecao.ComunicacaoException;
import com.msemprestimo.excecao.ConflitoException;
import com.msemprestimo.excecao.RecursoNaoEncontradoException;
import com.msemprestimo.excecao.RequisicaoInvalidaException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EquipamentoCondicaoClient {

	public static final String CIRCUIT_BREAKER = "msequipamentoCondicao";

	private final EquipamentoClient equipamentoClient;

	@CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "alterarCondicaoFallback")
	public void alterarCondicao(UUID equipamentoId, CondicaoEquipamento condicao) {
		try {
			equipamentoClient.alterarCondicao(equipamentoId, new CondicaoEquipamentoRequest(condicao));
		} catch (FeignException ex) {
			if (ex.status() >= 400 && ex.status() < 500) {
				throw erroDeNegocio(ex);
			}
			throw ex;
		}
	}

	@SuppressWarnings("unused")
	private void alterarCondicaoFallback(UUID equipamentoId, CondicaoEquipamento condicao, Throwable causa) {
		if (causa instanceof RecursoNaoEncontradoException negocio) {
			throw negocio;
		}
		if (causa instanceof ConflitoException negocio) {
			throw negocio;
		}
		if (causa instanceof RequisicaoInvalidaException negocio) {
			throw negocio;
		}
		log.warn("Indisponibilidade ao alterar a condição do equipamento {}", equipamentoId, causa);
		throw new ComunicacaoException("Serviço de equipamentos indisponível", HttpStatus.SERVICE_UNAVAILABLE);
	}

	private RuntimeException erroDeNegocio(FeignException ex) {
		if (ex.status() == HttpStatus.NOT_FOUND.value()) {
			return new RecursoNaoEncontradoException("Equipamento não encontrado");
		}
		if (ex.status() == HttpStatus.CONFLICT.value()) {
			return new ConflitoException("Não foi possível alterar a condição do equipamento");
		}
		return new RequisicaoInvalidaException("Condição do equipamento inválida");
	}

}
