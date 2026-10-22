package br.com.credjam.infrastructure.repository;

import br.com.credjam.domain.HistoricoAtualizacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoAtualizacaoRepository extends JpaRepository<HistoricoAtualizacaoEntity, Long> {
}
