package br.com.credjam.service;

import br.com.credjam.dto.Modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Regras de negócio do saque descritas na seção 6 do documento de regras do JAM:
 *
 * <ul>
 *   <li><b>Conta sem saldo:</b> se, após o cálculo, depósitos e JAM forem ambos zero, a
 *       conta é tratada como "sem saldo" e o saque é rejeitado — com exceção específica
 *       para conta tipo 4, categoria 6 e afastamento I2/I4.</li>
 *   <li><b>Reforma Trabalhista (acordo):</b> em contas tipo 2 com saque de acordo
 *       (código 07) e afastamento I5/I6, o valor do acordo é abatido primeiro do JAM e
 *       depois dos depósitos.</li>
 *   <li><b>Valor do saque:</b> derivado dos saldos corrigidos (JAM + depósitos do bolsão
 *       pertinente ao tipo de saque).</li>
 * </ul>
 */
public final class SaqueService {

    private static final int ESCALA = 2;
    private static final Set<String> AFASTAMENTOS_SEM_SALDO_PERMITIDO = Set.of("I2", "I4");
    private static final Set<String> AFASTAMENTOS_ACORDO = Set.of("I5", "I6");
    private static final String CODIGO_SAQUE_ACORDO = "07";

    /**
     * Apura o saque.
     *
     * @param saque         parâmetros do saque (pode ser nulo: nesse caso não há apuração).
     * @param tipoConta     COD-TIPO-CONTA da conta.
     * @param jamTotal      JAM total corrigido no período.
     * @param depositos     depósitos do bolsão pertinente ao saque.
     */
    public Modelo.SaqueResponse apurar(Modelo.SaqueRequest saque, String tipoConta,
                                       BigDecimal jamTotal, BigDecimal depositos) {
        if (saque == null) {
            return null;
        }
        BigDecimal jam = moeda(jamTotal);
        BigDecimal dep = moeda(depositos);

        // Conta sem saldo: depósitos e JAM ambos zero -> rejeita, salvo exceção
        // (tipo 4, categoria 6, afastamento I2/I4).
        boolean semSaldo = jam.signum() == 0 && dep.signum() == 0;
        if (semSaldo && !excecaoSemSaldo(saque, tipoConta)) {
            return new Modelo.SaqueResponse(true, zero(), zero(), zero(),
                "Conta sem saldo: depósitos e JAM zerados; saque rejeitado.");
        }

        // Reforma Trabalhista: abate o acordo primeiro do JAM, depois dos depósitos.
        if (isAcordoReformaTrabalhista(saque, tipoConta)) {
            BigDecimal acordo = moeda(saque.valorAcordo());
            BigDecimal abatidoJam = acordo.min(jam);
            BigDecimal restante = acordo.subtract(abatidoJam).setScale(ESCALA, RoundingMode.HALF_UP);
            BigDecimal abatidoDep = restante.min(dep);
            BigDecimal valorSaque = abatidoJam.add(abatidoDep).setScale(ESCALA, RoundingMode.HALF_UP);
            String obs = "Saque de acordo (Reforma Trabalhista): abatido primeiro do JAM ("
                + abatidoJam + ") e depois dos depósitos (" + abatidoDep + ").";
            return new Modelo.SaqueResponse(false, valorSaque, abatidoJam, abatidoDep, obs);
        }

        // Valor do saque padrão: JAM + depósitos do bolsão pertinente.
        BigDecimal valorSaque = jam.add(dep).setScale(ESCALA, RoundingMode.HALF_UP);
        return new Modelo.SaqueResponse(semSaldo, valorSaque, zero(), zero(),
            semSaldo ? "Conta sem saldo, mas liberada pela exceção tipo 4/categoria 6/afastamento I2-I4."
                     : "Valor do saque = JAM + depósitos do bolsão pertinente.");
    }

    private boolean excecaoSemSaldo(Modelo.SaqueRequest saque, String tipoConta) {
        return "4".equals(normalizar(tipoConta))
            && "6".equals(normalizar(saque.categoriaConta()))
            && AFASTAMENTOS_SEM_SALDO_PERMITIDO.contains(normalizarAfastamento(saque.codigoAfastamentoSaque()));
    }

    private boolean isAcordoReformaTrabalhista(Modelo.SaqueRequest saque, String tipoConta) {
        return "2".equals(normalizar(tipoConta))
            && CODIGO_SAQUE_ACORDO.equals(normalizar(saque.codigoSaque()))
            && AFASTAMENTOS_ACORDO.contains(normalizarAfastamento(saque.codigoAfastamentoSaque()))
            && saque.valorAcordo() != null && saque.valorAcordo().signum() > 0;
    }

    private static String normalizar(String v) {
        return v == null ? "" : v.trim();
    }

    private static String normalizarAfastamento(String v) {
        return v == null ? "" : v.trim().toUpperCase();
    }

    private static BigDecimal moeda(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(ESCALA);
    }
}
