package com.msemprestimo.emprestimo;

import com.msemprestimo.emprestimo.dto.DevolucaoRequest;
import com.msemprestimo.emprestimo.dto.EmprestimoRequest;
import com.msemprestimo.emprestimo.dto.EmprestimoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/emprestimos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EmprestimoController {

	private final EmprestimoService service;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<EmprestimoResponse> solicitar(@Valid @RequestBody EmprestimoRequest request) {
		EmprestimoResponse response = service.solicitar(request);
		return ResponseEntity.created(
				ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri()
		).body(response);
	}

	@GetMapping("/usuarios/{usuarioId}")
	public List<EmprestimoResponse> listarPorUsuario(@PathVariable UUID usuarioId) {
		return service.listarPorUsuario(usuarioId);
	}

	@GetMapping("/{id}")
	public EmprestimoResponse buscar(@PathVariable UUID id) {
		return service.buscarPorId(id);
	}

	@PatchMapping(path = "/{id}/devolucao", consumes = MediaType.APPLICATION_JSON_VALUE)
	public EmprestimoResponse devolver(@PathVariable UUID id, @Valid @RequestBody DevolucaoRequest request) {
		return service.devolver(id, request);
	}

}
