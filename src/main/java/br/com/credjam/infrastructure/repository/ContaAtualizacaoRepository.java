package br.com.credjam.infrastructure.repository;

import br.com.credjam.domain.ContaAtualizacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaAtualizacaoRepository extends JpaRepository<ContaAtualizacaoEntity, Long> {
}
