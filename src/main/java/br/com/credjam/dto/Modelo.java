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
        // Classe de remuneração fixa da conta (0, 3, 4, 5 ou 6). Atributo histórico do
        // vínculo: quando informado, prevalece sobre a derivação VERTAXA. A classe 0%
        // (só Atualização Monetária) só é alcançável por este campo.
        Integer classeRemuneracao,
        // Origem FGI: saque na vigência negativo é zerado em vez de rejeitar (seção 4/6).
        Boolean contaOrigemFgi,
        // Saldos por bolsão e parâmetros de saque (seção 4 e 6).
        Saldos saldos, SaqueRequest saque,
        List<LancamentoHistoricoRequest> lancamentosHistoricos
    ) {}

    /**
     * Os cinco "bolsões" de saldo corrigidos separadamente pelo JAM (seção 4 do documento).
     * Quando ausentes, o motor opera no modo saldo único (compatível com o comportamento
     * anterior). O saldo principal (SBPC), quando não informado, assume o saldoInicial.
     */
    public record Saldos(
        BigDecimal principalSbpc,       // Saldo principal (depósitos + JAM)
        BigDecimal saqueVigenciaSqvg,   // Saque na vigência (SQVG)
        BigDecimal saqueFmpSqfmp,       // Saque FMP (SQFMP)
        BigDecimal retornoFmp,          // Retorno FMP
        BigDecimal depositoRescisorio   // Depósito rescisório
    ) {}

    /**
     * Parâmetros do saque para as regras da seção 6. {@code valorAcordo} é usado no
     * abatimento da Reforma Trabalhista (tipo 2, código 07, afastamento I5/I6).
     */
    public record SaqueRequest(
        String codigoSaque, String categoriaConta, String codigoAfastamentoSaque,
        BigDecimal valorAcordo
    ) {}

    /** Bolsões corrigidos devolvidos no resultado. */
    public record SaldosResponse(
        BigDecimal principalSbpc, BigDecimal saqueVigenciaSqvg, BigDecimal saqueFmpSqfmp,
        BigDecimal retornoFmp, BigDecimal depositoRescisorio, BigDecimal totalCorrigido
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
        List<ItemMemoria> memoriaCalculo,
        // Bolsões corrigidos e resultado do saque (seções 4 e 6). Podem ser nulos
        // quando o cliente não informou saldos por bolsão nem parâmetros de saque.
        SaldosResponse saldosCorrigidos, SaqueResponse saque
    ) {}

    /** Resultado da apuração do saque (seção 6): valor pago e composição. */
    public record SaqueResponse(
        boolean contaSemSaldo, BigDecimal valorSaque, BigDecimal abatidoDoJam,
        BigDecimal abatidoDosDepositos, String observacao
    ) {}

    public record ErroResponse(String codigo, String mensagem, Object detalhes) {}
}
