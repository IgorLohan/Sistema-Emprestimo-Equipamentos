package com.msequipamento.equipamento;

import com.msequipamento.equipamento.dto.EquipamentoRequest;
import com.msequipamento.equipamento.dto.EquipamentoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/equipamentos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EquipamentoController {

	private final EquipamentoService service;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<EquipamentoResponse> criar(@Valid @RequestBody EquipamentoRequest request) {
		EquipamentoResponse response = service.criar(request);
		return ResponseEntity.created(
				ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri()
		).body(response);
	}

	@GetMapping("/disponiveis")
	public List<EquipamentoResponse> listarDisponiveis() {
		return service.listarDisponiveis();
	}

	@GetMapping("/{id}")
	public EquipamentoResponse buscar(@PathVariable UUID id) {
		return service.buscarPorId(id);
	}

	@PutMapping("/{id}/reservas/{emprestimoId}")
	public EquipamentoResponse reservar(@PathVariable UUID id, @PathVariable UUID emprestimoId) {
		return service.reservar(id, emprestimoId);
	}

	@DeleteMapping("/{id}/reservas/{emprestimoId}")
	public ResponseEntity<Void> liberar(@PathVariable UUID id, @PathVariable UUID emprestimoId) {
		service.liberar(id, emprestimoId);
		return ResponseEntity.noContent().build();
	}

}
