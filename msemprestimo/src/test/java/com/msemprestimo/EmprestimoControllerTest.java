package com.msemprestimo;

import com.msemprestimo.cliente.CondicaoEquipamento;
import com.msemprestimo.cliente.CondicaoEquipamentoRequest;
import com.msemprestimo.cliente.EquipamentoClient;
import com.msemprestimo.cliente.EquipamentoCondicaoClient;
import com.msemprestimo.cliente.EquipamentoResumo;
import com.msemprestimo.excecao.ComunicacaoException;
import com.msemprestimo.excecao.RecursoNaoEncontradoException;
import com.msemprestimo.cliente.UsuarioClient;
import com.msemprestimo.cliente.UsuarioResumo;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmprestimoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UsuarioClient usuarioClient;

	@MockitoBean
	private EquipamentoClient equipamentoClient;

	@Autowired
	private EquipamentoCondicaoClient equipamentoCondicaoClient;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@AfterEach
	void reiniciaCircuito() {
		circuitBreakerRegistry.circuitBreaker(EquipamentoCondicaoClient.CIRCUIT_BREAKER).reset();
	}

	@Test
	void solicitaReservaERegistraDevolucao() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		UUID equipamentoId = UUID.randomUUID();
		doReturn(new UsuarioResumo(usuarioId, "Ana Souza", "ana@escola.edu", "MAT1"))
				.when(usuarioClient).buscarPorId(usuarioId);
		doReturn(new EquipamentoResumo(equipamentoId, "Notebook", "Notebook", "PAT-1", true, null, CondicaoEquipamento.BOM))
				.when(equipamentoClient).buscarPorId(equipamentoId);

		String corpo = """
				{"usuarioId":"%s","equipamentoId":"%s"}
				""".formatted(usuarioId, equipamentoId);

		String resposta = mockMvc.perform(post("/api/v1/emprestimos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()))
				.andReturn().getResponse().getContentAsString();

		String emprestimoId = resposta.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		verify(equipamentoClient).reservar(eq(equipamentoId), eq(UUID.fromString(emprestimoId)));

		mockMvc.perform(get("/api/v1/emprestimos/{id}", emprestimoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVO"));

		mockMvc.perform(get("/api/v1/emprestimos/usuarios/{usuarioId}", usuarioId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(emprestimoId));

		mockMvc.perform(patch("/api/v1/emprestimos/{id}/devolucao", emprestimoId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"condicao":"AVARIADO"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DEVOLVIDO"))
				.andExpect(jsonPath("$.dataDevolucao").exists());

		mockMvc.perform(patch("/api/v1/emprestimos/{id}/devolucao", emprestimoId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"condicao":"BOM"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DEVOLVIDO"));

		verify(equipamentoClient).alterarCondicao(
				equipamentoId,
				new CondicaoEquipamentoRequest(CondicaoEquipamento.AVARIADO));
		verify(equipamentoClient).liberar(equipamentoId, UUID.fromString(emprestimoId));
	}

	@Test
	void recusaUsuarioOuEquipamentoInexistenteEEquipamentoOcupado() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		UUID equipamentoId = UUID.randomUUID();
		doThrow(erro(404)).when(usuarioClient).buscarPorId(usuarioId);

		mockMvc.perform(post("/api/v1/emprestimos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","equipamentoId":"%s"}
								""".formatted(usuarioId, equipamentoId)))
				.andExpect(status().isNotFound());
		verify(equipamentoClient, never()).reservar(any(), any());

		doReturn(new UsuarioResumo(usuarioId, "Ana", "ana@escola.edu", "MAT1"))
				.when(usuarioClient).buscarPorId(usuarioId);
		doThrow(erro(404)).when(equipamentoClient).buscarPorId(equipamentoId);

		mockMvc.perform(post("/api/v1/emprestimos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","equipamentoId":"%s"}
								""".formatted(usuarioId, equipamentoId)))
				.andExpect(status().isNotFound());

		doReturn(new EquipamentoResumo(equipamentoId, "Notebook", "Notebook", "PAT-1", false, UUID.randomUUID(), CondicaoEquipamento.BOM))
				.when(equipamentoClient).buscarPorId(equipamentoId);
		doThrow(erro(409)).when(equipamentoClient).reservar(eq(equipamentoId), any());

		mockMvc.perform(post("/api/v1/emprestimos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","equipamentoId":"%s"}
								""".formatted(usuarioId, equipamentoId)))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/emprestimos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":null,"equipamentoId":null}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void naoConcluiDevolucaoQuandoEquipamentoNaoExiste() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		UUID equipamentoId = UUID.randomUUID();
		String emprestimoId = criarEmprestimoAtivo(usuarioId, equipamentoId);
		doThrow(erro(404)).when(equipamentoClient).alterarCondicao(eq(equipamentoId), any());

		mockMvc.perform(patch("/api/v1/emprestimos/{id}/devolucao", emprestimoId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"condicao":"BOM"}
								"""))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/emprestimos/{id}", emprestimoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVO"));
		verify(equipamentoClient, never()).liberar(any(), any());
		assertEquals(CircuitBreaker.State.CLOSED, circuito().getState());
	}

	@Test
	void naoConcluiDevolucaoQuandoServicoDeEquipamentosFalha() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		UUID equipamentoId = UUID.randomUUID();
		String emprestimoId = criarEmprestimoAtivo(usuarioId, equipamentoId);
		doThrow(erro(503)).when(equipamentoClient).alterarCondicao(eq(equipamentoId), any());

		mockMvc.perform(patch("/api/v1/emprestimos/{id}/devolucao", emprestimoId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"condicao":"AVARIADO"}
								"""))
				.andExpect(status().isServiceUnavailable());

		mockMvc.perform(get("/api/v1/emprestimos/{id}", emprestimoId))
				.andExpect(jsonPath("$.status").value("ATIVO"));
		verify(equipamentoClient, never()).liberar(any(), any());
	}

	@Test
	void abreCircuitoDepoisDeFalhasTecnicas() {
		UUID equipamentoId = UUID.randomUUID();
		doThrow(erro(503)).when(equipamentoClient).alterarCondicao(eq(equipamentoId), any());

		for (int tentativa = 0; tentativa < 3; tentativa++) {
			assertThrows(ComunicacaoException.class,
					() -> equipamentoCondicaoClient.alterarCondicao(equipamentoId, CondicaoEquipamento.BOM));
		}

		assertEquals(CircuitBreaker.State.OPEN, circuito().getState());
		assertThrows(ComunicacaoException.class,
				() -> equipamentoCondicaoClient.alterarCondicao(equipamentoId, CondicaoEquipamento.AVARIADO));
		verify(equipamentoClient, times(3)).alterarCondicao(eq(equipamentoId), any());
	}

	@Test
	void erroDeNegocioNaoContaComoFalhaDoCircuito() {
		UUID equipamentoId = UUID.randomUUID();
		doThrow(erro(404)).when(equipamentoClient).alterarCondicao(eq(equipamentoId), any());

		for (int tentativa = 0; tentativa < 3; tentativa++) {
			assertThrows(RecursoNaoEncontradoException.class,
					() -> equipamentoCondicaoClient.alterarCondicao(equipamentoId, CondicaoEquipamento.BOM));
		}

		assertEquals(CircuitBreaker.State.CLOSED, circuito().getState());
	}

	private String criarEmprestimoAtivo(UUID usuarioId, UUID equipamentoId) throws Exception {
		doReturn(new UsuarioResumo(usuarioId, "Ana Souza", "ana@escola.edu", "MAT1"))
				.when(usuarioClient).buscarPorId(usuarioId);
		doReturn(new EquipamentoResumo(equipamentoId, "Notebook", "Notebook", "PAT-1", true, null, CondicaoEquipamento.BOM))
				.when(equipamentoClient).buscarPorId(equipamentoId);

		String resposta = mockMvc.perform(post("/api/v1/emprestimos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","equipamentoId":"%s"}
								""".formatted(usuarioId, equipamentoId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return resposta.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
	}

	private CircuitBreaker circuito() {
		return circuitBreakerRegistry.circuitBreaker(EquipamentoCondicaoClient.CIRCUIT_BREAKER);
	}

	private static FeignException erro(int status) {
		Request request = Request.create(
				Request.HttpMethod.GET,
				"http://localhost/api",
				Collections.emptyMap(),
				null,
				new RequestTemplate());
		Response response = Response.builder()
				.status(status)
				.reason("erro")
				.request(request)
				.headers(Collections.emptyMap())
				.body(new byte[0])
				.build();
		return FeignException.errorStatus("client#metodo", response);
	}

}
