package br.com.credjam.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Acumuladores históricos equivalentes aos VAL-JAM-DE-* do FGCD0080.
 * Uma instância deve ser criada por execução; não compartilha estado entre contas.
 */
public final class SBPC6973Service {
    private static final Set<LocalDate> DATAS_SBPC = Set.of(
        LocalDate.of(1969, 9, 30), LocalDate.of(1969, 12, 31),
        LocalDate.of(1970, 3, 31), LocalDate.of(1970, 6, 30),
        LocalDate.of(1970, 9, 30), LocalDate.of(1970, 12, 31),
        LocalDate.of(1971, 3, 31), LocalDate.of(1971, 6, 30),
        LocalDate.of(1971, 9, 30), LocalDate.of(1971, 12, 31),
        LocalDate.of(1972, 3, 31), LocalDate.of(1972, 6, 30),
        LocalDate.of(1972, 10, 2), LocalDate.of(1973, 1, 2)
    );
    /**
     * Códigos que disparam o acúmulo em SBPC6973 (ramo do corpo principal do FGCD0080):
     * COD-LANÇAMENTO IGUAL A 003 OU 005 OU 007.
     */
    private static final Set<String> CODIGOS_ELEGIVEIS = Set.of("003", "005", "007");

    /**
     * Códigos avaliados pela rotina CALCSBPC do FGCD0080 ao varrer o FGCRB560 para
     * determinar a DTA-CREDITO-JAM de cada lançamento:
     * 003, 005, 006, 007, 008, 780, 781, 782, 783.
     */
    private static final Set<String> CODIGOS_CALCSBPC =
        Set.of("003", "005", "006", "007", "008", "780", "781", "782", "783");

    /** Limites do período histórico SBPC: 30.09.1969 a 02.01.1973. */
    private static final LocalDate INICIO_PERIODO = LocalDate.of(1969, 9, 30);
    private static final LocalDate FIM_PERIODO = LocalDate.of(1973, 1, 2);

    private final Map<LocalDate, BigDecimal> acumuladores = new LinkedHashMap<>();

    public SBPC6973Service() {
        DATAS_SBPC.stream().sorted().forEach(d -> acumuladores.put(d, BigDecimal.ZERO.setScale(2)));
    }

    /** Replica SBPC6973: soma DEP + JAM no acumulador da data de crédito JAM. */
    public void acumular(LocalDate dataCreditoJam, String codigoLancamento,
                         BigDecimal deposito, BigDecimal jam) {
        if (dataCreditoJam == null || !DATAS_SBPC.contains(dataCreditoJam)) return;
        String codigo = normalizarCodigo(codigoLancamento);
        if (!CODIGOS_ELEGIVEIS.contains(codigo)) return;
        BigDecimal valor = zero(deposito).add(zero(jam)).setScale(2, java.math.RoundingMode.HALF_UP);
        acumuladores.merge(dataCreditoJam, valor, (a, b) -> a.add(b).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    /**
     * Replica JAM-6973: retorna o acumulador da data para ser somado ao saldo
     * (ADICIONAR VAL-JAM-DE-&lt;data&gt; AO VAL-SALDO). O COBOL NÃO zera o acumulador
     * nessa operação, portanto a leitura aqui também é não destrutiva.
     */
    public BigDecimal valorReincorporacao(LocalDate dataAtualizacaoCoeficiente) {
        return acumuladores.getOrDefault(dataAtualizacaoCoeficiente, BigDecimal.ZERO.setScale(2));
    }

    public Map<LocalDate, BigDecimal> fotografia() {
        return Map.copyOf(acumuladores);
    }

    /** Indica se a data pertence ao conjunto de datas-base do SBPC (extremos inclusive). */
    public static boolean dataHistorica(LocalDate data) {
        return data != null && DATAS_SBPC.contains(data);
    }

    /**
     * Condição do gatilho SBPC6973 no corpo principal do FGCD0080:
     * (DTA-CREDITO-JAM MAIOR QUE 30.09.1969) E (DTA-CREDITO-JAM MENOR QUE 02.01.1973)
     * E (COD-LANÇAMENTO = 003 OU 005 OU 007). Comparação estrita nos dois extremos.
     */
    public static boolean dentroPeriodoAcumulo(LocalDate data) {
        return data != null && data.isAfter(INICIO_PERIODO) && data.isBefore(FIM_PERIODO);
    }

    public static boolean isInicioPeriodo(LocalDate data) {
        return INICIO_PERIODO.equals(data);
    }

    public static boolean isFimPeriodo(LocalDate data) {
        return FIM_PERIODO.equals(data);
    }

    public static boolean codigoCalcSbpc(String codigoLancamento) {
        return CODIGOS_CALCSBPC.contains(normalizarCodigo(codigoLancamento));
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null) return "";
        String c = codigo.trim();
        return c.length() < 3 ? "0".repeat(3 - c.length()) + c : c;
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
