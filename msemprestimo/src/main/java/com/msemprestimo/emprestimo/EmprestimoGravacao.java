package com.msemprestimo.emprestimo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmprestimoGravacao {

	private final EmprestimoRepository repository;

	@Transactional
	public Emprestimo salvar(Emprestimo emprestimo) {
		return repository.saveAndFlush(emprestimo);
	}

	@Transactional
	public Optional<Emprestimo> marcarDevolvido(UUID id, LocalDateTime dataDevolucao) {
		return repository.findById(id).map(emprestimo -> {
			if (emprestimo.getStatus() == StatusEmprestimo.ATIVO) {
				emprestimo.setStatus(StatusEmprestimo.DEVOLVIDO);
				emprestimo.setDataDevolucao(dataDevolucao);
			}
			return emprestimo;
		});
	}

}
