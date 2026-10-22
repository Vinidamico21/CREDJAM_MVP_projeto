package br.com.credjam.infrastructure.repository;

import br.com.credjam.domain.CoeficienteJamEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoeficienteJamRepository extends JpaRepository<CoeficienteJamEntity, LocalDate> {
    @Query("select c from CoeficienteJamEntity c where c.dataAtualizacao > :ref order by c.dataAtualizacao")
    List<CoeficienteJamEntity> proximos(@Param("ref") LocalDate referencia);
}
