package com.msemprestimo;

import com.msemprestimo.cliente.EquipamentoClient;
import com.msemprestimo.cliente.EquipamentoResumo;
import com.msemprestimo.cliente.UsuarioClient;
import com.msemprestimo.cliente.UsuarioResumo;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

	@Test
	void solicitaReservaERegistraDevolucao() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		UUID equipamentoId = UUID.randomUUID();
		doReturn(new UsuarioResumo(usuarioId, "Ana Souza", "ana@escola.edu", "MAT1"))
				.when(usuarioClient).buscarPorId(usuarioId);
		doReturn(new EquipamentoResumo(equipamentoId, "Notebook", "Notebook", "PAT-1", true, null))
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

		mockMvc.perform(patch("/api/v1/emprestimos/{id}/devolucao", emprestimoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DEVOLVIDO"))
				.andExpect(jsonPath("$.dataDevolucao").exists());

		mockMvc.perform(patch("/api/v1/emprestimos/{id}/devolucao", emprestimoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DEVOLVIDO"));

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

		doReturn(new EquipamentoResumo(equipamentoId, "Notebook", "Notebook", "PAT-1", false, UUID.randomUUID()))
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
