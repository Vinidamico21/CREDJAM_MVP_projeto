package br.com.credjam.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Modelo {
    private Modelo() {}

    public record AtualizacaoRequest(
        String origem, LocalDate dataBase, LocalDate dataLimite,
        BigDecimal saldoInicial, BigDecimal totalDepositosInicial,
        BigDecimal jamInformadoInicial, BigDecimal saldoPosJamInicial,
        BigDecimal saldoComplementarInicial, LocalDate dataOpcao,
        String tipoConta, Boolean dadosHistoricosCompletos,
        String codigoAfastamento, LocalDate dataAfastamento,
        List<LancamentoHistoricoRequest> lancamentosHistoricos
    ) {}

    /**
     * Lançamento histórico informado pelo operador. A data de crédito do JAM
     * (DTA-CREDITO-JAM) NÃO é mais informada: ela é derivada no servidor pela
     * rotina CALCSBPC a partir do FGCRB560, usando a data de lançamento, a
     * competência e o código do lançamento.
     */
    public record LancamentoHistoricoRequest(
        LocalDate dataLancamento, LocalDate dataCompetencia, String codigoLancamento,
        BigDecimal valorDeposito, BigDecimal valorJam
    ) {}

    public record ItemMemoria(
        int sequencia, LocalDate dataCoeficiente, int taxaAnual,
        BigDecimal coeficienteAplicado, BigDecimal baseCalculo,
        BigDecimal jamPeriodo, BigDecimal saldoAnterior,
        BigDecimal saldoPosterior, String regraEspecial, String observacao
    ) {}

    public record ResultadoResponse(
        Long execucaoId, String status, String origem, LocalDate dataBase,
        LocalDate dataLimite, BigDecimal saldoInicial, BigDecimal saldoFinal,
        BigDecimal jamTotal, boolean dadosDemonstracao, List<String> avisos,
        List<ItemMemoria> memoriaCalculo
    ) {}

    public record ErroResponse(String codigo, String mensagem, Object detalhes) {}
}
