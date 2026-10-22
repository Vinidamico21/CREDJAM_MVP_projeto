package br.com.credjam.infrastructure.repository;

import br.com.credjam.domain.EvolucaoTaxaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvolucaoTaxaRepository extends JpaRepository<EvolucaoTaxaEntity, Long> {
}
