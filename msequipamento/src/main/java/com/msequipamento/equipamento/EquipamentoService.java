package com.msequipamento.equipamento;

import com.msequipamento.equipamento.dto.EquipamentoRequest;
import com.msequipamento.equipamento.dto.EquipamentoResponse;
import com.msequipamento.excecao.ConflitoException;
import com.msequipamento.excecao.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquipamentoService {

	private final EquipamentoRepository repository;

	@Transactional
	public EquipamentoResponse criar(EquipamentoRequest request) {
		String patrimonio = request.patrimonio().trim();
		if (repository.existsByPatrimonio(patrimonio)) {
			throw new ConflitoException("Patrimônio já cadastrado");
		}

		Equipamento equipamento = new Equipamento();
		equipamento.setNome(request.nome().trim());
		equipamento.setTipo(request.tipo().trim());
		equipamento.setPatrimonio(patrimonio);
		equipamento.setDisponivel(true);
		equipamento.setCondicao(CondicaoEquipamento.BOM);

		try {
			return toResponse(repository.saveAndFlush(equipamento));
		} catch (DataIntegrityViolationException ex) {
			throw new ConflitoException("Patrimônio já cadastrado");
		}
	}

	@Transactional(readOnly = true)
	public EquipamentoResponse buscarPorId(UUID id) {
		return repository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado"));
	}

	@Transactional(readOnly = true)
	public List<EquipamentoResponse> listarDisponiveis() {
		return repository.findByDisponivelTrueOrderByNomeAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public EquipamentoResponse reservar(UUID id, UUID emprestimoId) {
		Equipamento equipamento = repository.findByIdComBloqueio(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado"));

		if (emprestimoId.equals(equipamento.getReservaEmprestimoId())) {
			return toResponse(equipamento);
		}
		if (!equipamento.isDisponivel() || equipamento.getReservaEmprestimoId() != null) {
			throw new ConflitoException("Equipamento indisponível");
		}

		equipamento.setDisponivel(false);
		equipamento.setReservaEmprestimoId(emprestimoId);
		return toResponse(equipamento);
	}

	@Transactional
	public void liberar(UUID id, UUID emprestimoId) {
		Equipamento equipamento = repository.findByIdComBloqueio(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado"));

		if (emprestimoId.equals(equipamento.getReservaEmprestimoId())) {
			equipamento.setDisponivel(true);
			equipamento.setReservaEmprestimoId(null);
			return;
		}
		if (equipamento.isDisponivel() && equipamento.getReservaEmprestimoId() == null) {
			return;
		}
		throw new ConflitoException("Equipamento reservado por outro empréstimo");
	}

	@Transactional
	public EquipamentoResponse atualizarCondicao(UUID id, CondicaoEquipamento condicao) {
		Equipamento equipamento = repository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado"));
		equipamento.setCondicao(condicao);
		return toResponse(equipamento);
	}

	private EquipamentoResponse toResponse(Equipamento equipamento) {
		return new EquipamentoResponse(
				equipamento.getId(),
				equipamento.getNome(),
				equipamento.getTipo(),
				equipamento.getPatrimonio(),
				equipamento.isDisponivel(),
				equipamento.getReservaEmprestimoId(),
				equipamento.getCondicao()
		);
	}

}
