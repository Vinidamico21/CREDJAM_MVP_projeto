package br.com.credjam.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transições de moeda observadas na rotina CONVERTE do FGCD0080.
 * Aplica somente transições cruzadas entre dataAnterior e dataAtual; não repete conversões.
 */
public final class ConversaoMonetariaService {
    private static final List<Transicao> TRANSICOES = List.of(
        new Transicao(LocalDate.of(1984, 8, 15), new BigDecimal("100"), "CONVERSAO_1984", "Conversão monetária /100 (Cruzeiro); conforme documento de regras do JAM"),
        new Transicao(LocalDate.of(1986, 2, 27), new BigDecimal("1000"), "CONVERSAO_1986", "Conversão monetária /1000; movimentos legados 990–994"),
        new Transicao(LocalDate.of(1989, 1, 15), new BigDecimal("1000"), "CONVERSAO_1989", "Conversão monetária /1000; movimentos legados 990–994"),
        new Transicao(LocalDate.of(1993, 8, 1), new BigDecimal("1000"), "CONVERSAO_1993", "Conversão monetária /1000; movimentos legados 097–098"),
        new Transicao(LocalDate.of(1994, 7, 1), new BigDecimal("2750"), "CONVERSAO_1994", "Conversão monetária /2750; movimentos legados 095–096")
    );

    public Resultado aplicarTransicoes(LocalDate dataAnterior, LocalDate dataAtual, Estado estadoInicial) {
        Estado estado = estadoInicial.normalizado();
        List<EventoConversao> eventos = new ArrayList<>();
        if (dataAnterior == null || dataAtual == null || !dataAtual.isAfter(dataAnterior)) {
            return new Resultado(estado, eventos);
        }
        for (Transicao t : TRANSICOES) {
            // O COBOL usa DTA-CONVERSAO > data histórica e DTA-ULT-CONVERSAO < data histórica.
            if (dataAnterior.isBefore(t.data()) && dataAtual.isAfter(t.data())) {
                if (t.fator().compareTo(BigDecimal.ONE) > 0) estado = estado.dividir(t.fator());
                else estado = estado.normalizado();
                eventos.add(new EventoConversao(t.data(), t.codigo(), t.descricao(), t.fator()));
            }
        }
        return new Resultado(estado, List.copyOf(eventos));
    }

    private static BigDecimal moeda(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private record Transicao(LocalDate data, BigDecimal fator, String codigo, String descricao) {}
    public record EventoConversao(LocalDate data, String codigo, String descricao, BigDecimal fator) {}
    public record Resultado(Estado estado, List<EventoConversao> eventos) {}
    public record Estado(BigDecimal saldo, BigDecimal saldoPosJam, BigDecimal jamAcumulado,
                         BigDecimal totalDepositos, BigDecimal jamInformado, BigDecimal saldoComplementar) {
        Estado normalizado() {
            return new Estado(moeda(saldo), moeda(saldoPosJam), moeda(jamAcumulado),
                totalDepositos == null ? null : moeda(totalDepositos),
                jamInformado == null ? null : moeda(jamInformado), moeda(saldoComplementar));
        }
        Estado dividir(BigDecimal fator) {
            return new Estado(dividir(saldo, fator), dividir(saldoPosJam, fator), dividir(jamAcumulado, fator),
                totalDepositos == null ? null : dividir(totalDepositos, fator),
                jamInformado == null ? null : dividir(jamInformado, fator), dividir(saldoComplementar, fator));
        }
        private static BigDecimal dividir(BigDecimal valor, BigDecimal fator) {
            return moeda(valor).divide(fator, 2, RoundingMode.HALF_UP);
        }
    }
}
