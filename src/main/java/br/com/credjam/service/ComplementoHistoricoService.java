package br.com.credjam.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Regra literal do FGCD0080 para o complemento de 01/04/1976. */
public final class ComplementoHistoricoService {
    private static final LocalDate DATA_COMPLEMENTO = LocalDate.of(1976, 4, 1);
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.113000");

    public Resultado aplicar(LocalDate dataCoeficiente, BigDecimal saldo,
                             BigDecimal saldoComplementar) {
        BigDecimal saldoSeguro = moeda(saldo);
        BigDecimal complementar = moeda(saldoComplementar);
        if (!DATA_COMPLEMENTO.equals(dataCoeficiente) || complementar.signum() == 0) {
            return new Resultado(saldoSeguro, BigDecimal.ZERO.setScale(2), false);
        }
        BigDecimal acrescimo = complementar.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_UP);
        return new Resultado(saldoSeguro.add(acrescimo).setScale(2, RoundingMode.HALF_UP), acrescimo, true);
    }

    private static BigDecimal moeda(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
    public record Resultado(BigDecimal saldo, BigDecimal valorComplemento, boolean aplicado) {}
}
