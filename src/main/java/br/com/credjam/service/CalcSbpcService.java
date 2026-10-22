package br.com.credjam.service;

import br.com.credjam.domain.CalendarioSbpcEntity;
import br.com.credjam.infrastructure.repository.CalendarioSbpcRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/**
 * Equivalente à rotina CALCSBPC do FGCD0080.
 *
 * <p>No COBOL, CALCSBPC varre o arquivo FGCRB560 (via CTRL-SBPC) e, para os códigos
 * de lançamento elegíveis (003, 005, 006, 007, 008, 780..783), determina a
 * DTA-CREDITO-JAM a partir da linha do calendário cuja DTA-SBPC é a primeira igual
 * ou posterior à data do lançamento, respeitando as regras de competência e as
 * exceções de 31.10.1989 e 06.09.1991 para contas não judiciais.</p>
 *
 * <p>A data de crédito deixa de ser informada pelo operador e passa a ser derivada
 * aqui, com base no FGCRB560, fiel ao legado.</p>
 */
@Service
public class CalcSbpcService {
    private static final LocalDate CORTE_COMPETENCIA = LocalDate.of(1989, 10, 31);
    private static final LocalDate CORTE_DIA10 = LocalDate.of(1991, 9, 6);

    private final CalendarioSbpcRepository calendario;

    public CalcSbpcService(CalendarioSbpcRepository calendario) {
        this.calendario = calendario;
    }

    /**
     * Resultado da derivação: a data de crédito JAM calculada (ou {@code null} quando
     * o lançamento não é elegível ou não há linha correspondente no FGCRB560) e se o
     * calendário FGCRB560 estava carregado.
     */
    public record Resultado(LocalDate dataCreditoJam, boolean calendarioDisponivel, String motivo) {
        public boolean derivada() { return dataCreditoJam != null; }
    }

    /**
     * Deriva a DTA-CREDITO-JAM de um lançamento, espelhando a seleção da CALCSBPC.
     *
     * @param codigoLancamento código do lançamento (ex.: "003").
     * @param dataLancamento   DTA-LANÇAMENTO do lançamento.
     * @param dataCompetencia  DTA-COMPETÊNCIA do lançamento (pode ser nula).
     * @param contaJudicial    indica se é conta judicial (FLAG-CTA-JUDICIAL = 'S').
     */
    public Resultado derivar(String codigoLancamento, LocalDate dataLancamento,
                             LocalDate dataCompetencia, boolean contaJudicial) {
        if (dataLancamento == null) {
            return new Resultado(null, true, "Data de lançamento ausente.");
        }
        if (!SBPC6973Service.codigoCalcSbpc(codigoLancamento)) {
            return new Resultado(null, true, "Código de lançamento fora do conjunto da CALCSBPC.");
        }

        List<CalendarioSbpcEntity> linhas = calendario.apartirDe(dataLancamento);
        if (linhas.isEmpty()) {
            return new Resultado(null, false, "FGCRB560 sem linhas aplicáveis para a data informada.");
        }

        for (CalendarioSbpcEntity linha : linhas) {
            if (!selecionavel(linha, dataLancamento, dataCompetencia, contaJudicial)) {
                continue; // OBTER PRÓXIMO FGCRB560 (pula a linha conforme as exceções)
            }
            if (linha.getDataCreditoJam() != null) {
                return new Resultado(linha.getDataCreditoJam(), true, "Derivada do FGCRB560 pela CALCSBPC.");
            }
        }
        return new Resultado(null, true, "Nenhuma linha do FGCRB560 satisfez as regras da CALCSBPC.");
    }

    /**
     * Replica a decisão da CALCSBPC de pular ou aceitar uma linha do FGCRB560.
     *
     * COBOL:
     *   IF (DTA-SBPC > DTA-LANÇAMENTO) OU ((DTA-SBPC = DTA-LANÇAMENTO) E (DTA-RECOLHIMENTO = DTA-LANÇAMENTO))
     *      SE (CONTA NÃO JUDICIAL E DTA-COMPETÊNCIA < DTA-COMPETÊNCIA-FGCRB560 E DTA-LANÇAMENTO > 31.10.1989)
     *         OU (DTA-LANÇAMENTO > 06.09.1991 E DIA-LANÇAMENTO = 10)
     *         OBTER PRÓXIMO FGCRB560   (pula)
     */
    private boolean selecionavel(CalendarioSbpcEntity linha, LocalDate dataLancamento,
                                 LocalDate dataCompetencia, boolean contaJudicial) {
        LocalDate dataSbpc = linha.getDataSbpc();
        LocalDate dataRecolhimento = linha.getDataRecolhimento();
        if (dataSbpc == null) return false;

        boolean janelaValida = dataSbpc.isAfter(dataLancamento)
            || (dataSbpc.isEqual(dataLancamento) && dataLancamento.equals(dataRecolhimento));
        if (!janelaValida) return false;

        boolean pulaPorCompetencia = !contaJudicial
            && dataCompetencia != null && linha.getDataSbpc() != null
            && dataCompetencia.isBefore(dataSbpc)
            && dataLancamento.isAfter(CORTE_COMPETENCIA);
        boolean pulaPorDia10 = dataLancamento.isAfter(CORTE_DIA10)
            && dataLancamento.getDayOfMonth() == 10;

        return !(pulaPorCompetencia || pulaPorDia10);
    }
}
