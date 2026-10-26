package br.com.credjam.service;

import br.com.credjam.dto.Modelo;
import br.com.credjam.exception.ErroNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Corrige os cinco "bolsões" de saldo descritos na seção 4 do documento de regras do JAM:
 * saldo principal (SBPC), saque na vigência (SQVG), saque FMP (SQFMP), retorno FMP e
 * depósito rescisório. Cada bolsão é atualizado pelo MESMO coeficiente mensal da classe
 * da conta, porém sobre a sua própria base, acumulando de forma composta mês a mês.
 *
 * <p>Regra de parada (seção 4/6): se qualquer bolsão ficar NEGATIVO durante o cálculo, o
 * processo é interrompido com erro. Exceção para contas de origem FGI, em que o saque na
 * vigência negativo é zerado em vez de rejeitado.</p>
 */
public final class BolsoesService {

    private static final int ESCALA = 2;

    /** Estado corrente dos cinco bolsões. Imutável a cada passo. */
    public record Estado(
        BigDecimal principalSbpc, BigDecimal saqueVigenciaSqvg, BigDecimal saqueFmpSqfmp,
        BigDecimal retornoFmp, BigDecimal depositoRescisorio) {

        public static Estado de(Modelo.Saldos s, BigDecimal principalPadrao) {
            if (s == null) {
                return new Estado(moeda(principalPadrao), zero(), zero(), zero(), zero());
            }
            BigDecimal principal = s.principalSbpc() != null ? s.principalSbpc() : principalPadrao;
            return new Estado(
                moeda(principal), moeda(s.saqueVigenciaSqvg()), moeda(s.saqueFmpSqfmp()),
                moeda(s.retornoFmp()), moeda(s.depositoRescisorio()));
        }

        public BigDecimal total() {
            return principalSbpc.add(saqueVigenciaSqvg).add(saqueFmpSqfmp)
                .add(retornoFmp).add(depositoRescisorio).setScale(ESCALA, RoundingMode.HALF_UP);
        }

        public Modelo.SaldosResponse toResponse() {
            return new Modelo.SaldosResponse(principalSbpc, saqueVigenciaSqvg, saqueFmpSqfmp,
                retornoFmp, depositoRescisorio, total());
        }
    }

    /**
     * Aplica o incremento do coeficiente do mês a cada bolsão (JAM do bolsão = base × coef,
     * novo saldo = base + JAM) e valida a regra de saldo negativo.
     *
     * @param coeficienteIncremento incremento do mês (coeficiente - 1), como armazenado na FGCRB550.
     * @param contaOrigemFgi        quando true, zera SQVG negativo em vez de rejeitar.
     */
    public Estado aplicarMes(Estado atual, BigDecimal coeficienteIncremento, boolean contaOrigemFgi) {
        BigDecimal principal = capitalizar(atual.principalSbpc(), coeficienteIncremento);
        BigDecimal sqvg = capitalizar(atual.saqueVigenciaSqvg(), coeficienteIncremento);
        BigDecimal sqfmp = capitalizar(atual.saqueFmpSqfmp(), coeficienteIncremento);
        BigDecimal retFmp = capitalizar(atual.retornoFmp(), coeficienteIncremento);
        BigDecimal depRes = capitalizar(atual.depositoRescisorio(), coeficienteIncremento);

        // Exceção FGI: saque na vigência negativo é zerado, não rejeitado.
        if (sqvg.signum() < 0) {
            if (contaOrigemFgi) {
                sqvg = zero();
            } else {
                throw new ErroNegocioException("BOLSAO_NEGATIVO",
                    "Saque na vigência (SQVG) ficou negativo durante o cálculo do JAM.");
            }
        }
        exigirNaoNegativo("Saque FMP (SQFMP)", sqfmp);
        exigirNaoNegativo("Retorno FMP", retFmp);
        exigirNaoNegativo("Depósito rescisório", depRes);
        exigirNaoNegativo("Saldo principal (SBPC)", principal);

        return new Estado(principal, sqvg, sqfmp, retFmp, depRes);
    }

    private static BigDecimal capitalizar(BigDecimal base, BigDecimal coeficienteIncremento) {
        if (base == null || base.signum() == 0 || coeficienteIncremento == null) {
            return moeda(base);
        }
        BigDecimal jam = base.multiply(coeficienteIncremento).setScale(ESCALA, RoundingMode.HALF_UP);
        return base.add(jam).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static void exigirNaoNegativo(String nome, BigDecimal valor) {
        if (valor.signum() < 0) {
            throw new ErroNegocioException("BOLSAO_NEGATIVO",
                nome + " ficou negativo durante o cálculo do JAM; processo interrompido.");
        }
    }

    private static BigDecimal moeda(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(ESCALA);
    }
}
