package br.com.credjam;

import br.com.credjam.domain.CalendarioSbpcEntity;
import br.com.credjam.infrastructure.repository.CalendarioSbpcRepository;
import br.com.credjam.service.CalcSbpcService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica a derivação da DTA-CREDITO-JAM pela CALCSBPC a partir do FGCRB560,
 * espelhando o comportamento do FGCD0080.
 */
class CalcSbpcServiceTest {

    /** Repositório em memória que implementa apenas a consulta usada pela CALCSBPC. */
    private static CalcSbpcService comLinhas(List<CalendarioSbpcEntity> linhas) {
        CalendarioSbpcRepository repo = new FakeRepo(linhas);
        return new CalcSbpcService(repo);
    }

    private static CalendarioSbpcEntity linha(String dataSbpc, String recolhimento,
                                              String creditoJam, String codigo) {
        return CalendarioSbpcEntity.de(LocalDate.parse(dataSbpc), LocalDate.parse(recolhimento),
            LocalDate.parse(creditoJam), codigo, "DEMONSTRACAO", "DEMONSTRACAO", "TESTE");
    }

    @Test void derivaDataCreditoJamDaPrimeiraLinhaElegivel() {
        var calc = comLinhas(List.of(
            linha("1970-03-31", "1970-03-31", "1970-03-31", "003"),
            linha("1970-06-30", "1970-06-30", "1970-06-30", "003")));
        var r = calc.derivar("003", LocalDate.of(1970, 1, 15), null, false);
        assertTrue(r.derivada());
        assertTrue(r.calendarioDisponivel());
        assertEquals(LocalDate.of(1970, 3, 31), r.dataCreditoJam());
    }

    @Test void naoDerivaParaCodigoForaDaCalcSbpc() {
        var calc = comLinhas(List.of(linha("1970-03-31", "1970-03-31", "1970-03-31", "003")));
        var r = calc.derivar("999", LocalDate.of(1970, 1, 15), null, false);
        assertFalse(r.derivada());
        assertTrue(r.calendarioDisponivel());
    }

    @Test void sinalizaCalendarioAusenteQuandoFgcrb560Vazio() {
        var calc = comLinhas(List.of());
        var r = calc.derivar("003", LocalDate.of(1970, 1, 15), null, false);
        assertFalse(r.derivada());
        assertFalse(r.calendarioDisponivel());
    }

    @Test void excecaoDoDia10AposSetembro1991PulaLancamento() {
        // COBOL: lançamento em dia 10 após 06.09.1991 dispara OBTER PRÓXIMO em todas as
        // linhas (a exceção é do lançamento, não da linha); nenhuma data é derivada.
        var calc = comLinhas(List.of(
            linha("1991-10-10", "1991-10-10", "1991-10-10", "003"),
            linha("1991-11-30", "1991-11-30", "1972-06-30", "003")));
        var r = calc.derivar("003", LocalDate.of(1991, 10, 10), null, false);
        assertFalse(r.derivada());
        assertTrue(r.calendarioDisponivel());
    }

    @Test void lancamentoNormalNaoDia10DerivaNormalmente() {
        // Mesmo após 1991, um lançamento que não cai no dia 10 deriva normalmente.
        var calc = comLinhas(List.of(linha("1991-11-30", "1991-11-30", "1972-06-30", "003")));
        var r = calc.derivar("003", LocalDate.of(1991, 10, 15), null, false);
        assertTrue(r.derivada());
        assertEquals(LocalDate.of(1972, 6, 30), r.dataCreditoJam());
    }

    /** Implementação mínima: só a query apartirDe é usada pela CALCSBPC. */
    private static final class FakeRepo implements CalendarioSbpcRepository {
        private final List<CalendarioSbpcEntity> dados;
        FakeRepo(List<CalendarioSbpcEntity> dados) { this.dados = new ArrayList<>(dados); }

        @Override public List<CalendarioSbpcEntity> apartirDe(LocalDate dataLancamento) {
            return dados.stream()
                .filter(e -> e.getDataSbpc() != null && !e.getDataSbpc().isBefore(dataLancamento))
                .sorted(Comparator.comparing(CalendarioSbpcEntity::getDataSbpc)
                    .thenComparing(e -> e.getDataRecolhimento() == null ? LocalDate.MIN : e.getDataRecolhimento()))
                .toList();
        }

        // Métodos não usados pela CALCSBPC.
        @Override public <S extends CalendarioSbpcEntity> S save(S entity) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public java.util.Optional<CalendarioSbpcEntity> findById(Long id) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(Long id) { throw new UnsupportedOperationException(); }
        @Override public List<CalendarioSbpcEntity> findAll() { return List.copyOf(dados); }
        @Override public List<CalendarioSbpcEntity> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return dados.size(); }
        @Override public void deleteById(Long id) { throw new UnsupportedOperationException(); }
        @Override public void delete(CalendarioSbpcEntity entity) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends CalendarioSbpcEntity> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { throw new UnsupportedOperationException(); }
        @Override public void flush() { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> S saveAndFlush(S entity) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<CalendarioSbpcEntity> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public CalendarioSbpcEntity getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public CalendarioSbpcEntity getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public CalendarioSbpcEntity getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends CalendarioSbpcEntity, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public List<CalendarioSbpcEntity> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<CalendarioSbpcEntity> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
    }
}
