package com.msequipamento.equipamento;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID> {

	boolean existsByPatrimonio(String patrimonio);

	List<Equipamento> findByDisponivelTrueOrderByNomeAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Equipamento e where e.id = :id")
	Optional<Equipamento> findByIdComBloqueio(@Param("id") UUID id);

}
