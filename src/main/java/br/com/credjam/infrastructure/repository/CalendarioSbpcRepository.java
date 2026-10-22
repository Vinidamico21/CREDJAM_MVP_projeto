package br.com.credjam.infrastructure.repository;

import br.com.credjam.domain.CalendarioSbpcEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarioSbpcRepository extends JpaRepository<CalendarioSbpcEntity, Long> {

    /**
     * Equivalente à varredura da rotina CALCSBPC sobre o FGCRB560 via CTRL-SBPC:
     * retorna, em ordem de DATA_SBPC, as linhas do calendário cuja data de SBPC
     * é igual ou posterior à data do lançamento. A primeira linha é a que o COBOL
     * seleciona (primeira DTA-SBPC maior ou igual à DTA-LANÇAMENTO).
     */
    @Query("select c from CalendarioSbpcEntity c "
         + "where c.dataSbpc >= :dataLancamento order by c.dataSbpc asc, c.dataRecolhimento asc")
    List<CalendarioSbpcEntity> apartirDe(@Param("dataLancamento") LocalDate dataLancamento);
}
