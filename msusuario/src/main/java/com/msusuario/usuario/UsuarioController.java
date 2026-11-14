package com.msusuario.usuario;

import com.msusuario.usuario.dto.UsuarioRequest;
import com.msusuario.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UsuarioController {

	private final UsuarioService service;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
		UsuarioResponse response = service.criar(request);
		return ResponseEntity.created(
				ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri()
		).body(response);
	}

	@GetMapping("/{id}")
	public UsuarioResponse buscar(@PathVariable UUID id) {
		return service.buscarPorId(id);
	}

}
