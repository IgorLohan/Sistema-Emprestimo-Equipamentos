package com.msusuario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void cadastraEConsultaUsuario() throws Exception {
		String corpo = """
				{"nome":"Ana Souza","email":"ana.consulta@escola.edu","matricula":"MAT-CONSULTA"}
				""";

		String resposta = mockMvc.perform(post("/api/v1/usuarios")
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$.email").value("ana.consulta@escola.edu"))
				.andReturn().getResponse().getContentAsString();

		String id = resposta.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

		mockMvc.perform(get("/api/v1/usuarios/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matricula").value("MAT-CONSULTA"));
	}

	@Test
	void recusaEmailDuplicadoEDadosInvalidos() throws Exception {
		String corpo = """
				{"nome":"Bruno Lima","email":"Bruno.Lima@escola.edu","matricula":"MAT-DUP"}
				""";

		mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("bruno.lima@escola.edu"));

		mockMvc.perform(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/usuarios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome":"","email":"invalido","matricula":""}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.email").exists());

		mockMvc.perform(get("/api/v1/usuarios/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

}
