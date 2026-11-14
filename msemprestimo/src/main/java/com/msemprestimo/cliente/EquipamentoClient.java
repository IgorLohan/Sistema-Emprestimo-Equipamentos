package com.msemprestimo.cliente;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "msequipamento")
public interface EquipamentoClient {

	@GetMapping("/api/v1/equipamentos/{id}")
	EquipamentoResumo buscarPorId(@PathVariable("id") UUID id);

	@PutMapping("/api/v1/equipamentos/{id}/condicao")
	void alterarCondicao(@PathVariable("id") UUID id, @RequestBody CondicaoEquipamentoRequest request);

	@PutMapping("/api/v1/equipamentos/{id}/reservas/{emprestimoId}")
	void reservar(@PathVariable("id") UUID id, @PathVariable("emprestimoId") UUID emprestimoId);

	@DeleteMapping("/api/v1/equipamentos/{id}/reservas/{emprestimoId}")
	void liberar(@PathVariable("id") UUID id, @PathVariable("emprestimoId") UUID emprestimoId);

}
