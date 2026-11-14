package com.msequipamento;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EquipamentoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void reservaDeFormaIdempotenteELibera() throws Exception {
		String resposta = mockMvc.perform(post("/api/v1/equipamentos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"Projetor Sala 2","tipo":"Projetor","patrimonio":"PAT-RESERVA"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.disponivel").value(true))
				.andExpect(jsonPath("$.condicao").value("BOM"))
				.andReturn().getResponse().getContentAsString();

		String id = resposta.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		UUID emprestimoId = UUID.randomUUID();
		UUID outroEmprestimo = UUID.randomUUID();

		mockMvc.perform(get("/api/v1/equipamentos/disponiveis"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).exists());

		mockMvc.perform(put("/api/v1/equipamentos/{id}/reservas/{emprestimoId}", id, emprestimoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponivel").value(false))
				.andExpect(jsonPath("$.reservaEmprestimoId").value(emprestimoId.toString()));

		mockMvc.perform(put("/api/v1/equipamentos/{id}/reservas/{emprestimoId}", id, emprestimoId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reservaEmprestimoId").value(emprestimoId.toString()));

		mockMvc.perform(put("/api/v1/equipamentos/{id}/reservas/{emprestimoId}", id, outroEmprestimo))
				.andExpect(status().isConflict());

		mockMvc.perform(delete("/api/v1/equipamentos/{id}/reservas/{emprestimoId}", id, emprestimoId))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/equipamentos/{id}/reservas/{emprestimoId}", id, emprestimoId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/equipamentos/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponivel").value(true));

		mockMvc.perform(put("/api/v1/equipamentos/{id}/condicao", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"condicao":"AVARIADO"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.condicao").value("AVARIADO"))
				.andExpect(jsonPath("$.disponivel").value(true));
	}

	@Test
	void recusaPatrimonioDuplicadoEEquipamentoInexistente() throws Exception {
		String corpo = """
				{"nome":"Tablet 01","tipo":"Tablet","patrimonio":"PAT-DUP"}
				""";

		mockMvc.perform(post("/api/v1/equipamentos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/equipamentos").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isConflict());

		mockMvc.perform(put("/api/v1/equipamentos/{id}/reservas/{emprestimoId}", UUID.randomUUID(), UUID.randomUUID()))
				.andExpect(status().isNotFound());

		mockMvc.perform(put("/api/v1/equipamentos/{id}/condicao", UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"condicao":"BOM"}
								"""))
				.andExpect(status().isNotFound());
	}

}
