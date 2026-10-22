package br.com.credjam.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Equivalente à rotina JAM-6973: reincorpora o acumulador da data ao saldo. */
public final class JAM6973Service {
    public BigDecimal reincorporar(LocalDate dataAtualizacaoCoeficiente,
                                   BigDecimal saldo, SBPC6973Service acumuladores) {
        // JAM-6973 (COBOL): ADICIONAR VAL-JAM-DE-<data> AO VAL-SALDO. O acumulador
        // não é zerado; cada data de atualização de coeficiente é processada uma única vez.
        BigDecimal valorHistorico = acumuladores.valorReincorporacao(dataAtualizacaoCoeficiente);
        return saldo.add(valorHistorico).setScale(2, RoundingMode.HALF_UP);
    }
}
