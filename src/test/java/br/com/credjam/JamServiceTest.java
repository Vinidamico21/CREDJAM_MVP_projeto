package br.com.credjam;

import br.com.credjam.service.ComplementoHistoricoService;
import br.com.credjam.service.ConversaoMonetariaService;
import br.com.credjam.service.JAM6973Service;
import br.com.credjam.service.JamService;
import br.com.credjam.service.SBPC6973Service;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class JamServiceTest {
    private final JamService service = new JamService();
    @Test void calculaJamComPrecisaoMonetaria() {
        assertEquals(new BigDecimal("200.00"), service.calcular(new BigDecimal("20000.00"), new BigDecimal("0.01")));
        assertEquals(new BigDecimal("242.40"), service.calcular(new BigDecimal("20200.00"), new BigDecimal("0.012")));
    }

    @Test void complemento1976IncideSomenteSobreSaldoComplementar() {
        ComplementoHistoricoService regra = new ComplementoHistoricoService();
        var resultado = regra.aplicar(LocalDate.of(1976, 4, 1),
            new BigDecimal("1000.00"), new BigDecimal("200.00"));
        assertTrue(resultado.aplicado());
        assertEquals(new BigDecimal("22.60"), resultado.valorComplemento());
        assertEquals(new BigDecimal("1022.60"), resultado.saldo());
    }

    @Test void complemento1976NaoAplicaEmOutraData() {
        var resultado = new ComplementoHistoricoService().aplicar(LocalDate.of(1976, 4, 2),
            new BigDecimal("1000.00"), new BigDecimal("200.00"));
        assertFalse(resultado.aplicado());
        assertEquals(new BigDecimal("1000.00"), resultado.saldo());
    }

    @Test void conversao1986AplicaApenasQuandoCruzaDataHistorica() {
        var estado = new ConversaoMonetariaService.Estado(new BigDecimal("12345.00"),
            new BigDecimal("2000.00"), new BigDecimal("1000.00"),
            new BigDecimal("10000.00"), new BigDecimal("500.00"), BigDecimal.ZERO);
        var resultado = new ConversaoMonetariaService().aplicarTransicoes(
            LocalDate.of(1986, 2, 1), LocalDate.of(1986, 3, 1), estado);
        assertEquals(new BigDecimal("12.35"), resultado.estado().saldo());
        assertEquals(new BigDecimal("2.00"), resultado.estado().saldoPosJam());
        assertEquals("CONVERSAO_1986", resultado.eventos().get(0).codigo());
    }

    @Test void sbpc6973AcumulaSomenteCodigosElegiveis() {
        var sbpc = new SBPC6973Service();
        var data = LocalDate.of(1970, 3, 31);
        sbpc.acumular(data, "003", new BigDecimal("100.00"), new BigDecimal("5.00"));
        sbpc.acumular(data, "007", new BigDecimal("20.00"), new BigDecimal("1.00"));
        sbpc.acumular(data, "999", new BigDecimal("900.00"), BigDecimal.ZERO);
        // Apenas 003/005/007 disparam o acúmulo; o código 999 é ignorado.
        assertEquals(new BigDecimal("126.00"), sbpc.fotografia().get(data));
    }

    @Test void jam6973ReincorporaSemZerarAcumulador() {
        // COBOL JAM-6973 faz ADICIONAR ... AO VAL-SALDO, sem zerar o acumulador.
        var sbpc = new SBPC6973Service();
        var data = LocalDate.of(1970, 3, 31);
        sbpc.acumular(data, "003", new BigDecimal("100.00"), new BigDecimal("5.00"));
        var regra = new JAM6973Service();
        assertEquals(new BigDecimal("105.00"), regra.reincorporar(data, new BigDecimal("0.00"), sbpc));
        // Reincorporação não destrutiva: o acumulador permanece íntegro.
        assertEquals(new BigDecimal("105.00"), sbpc.fotografia().get(data));
    }

    @Test void periodoAcumuloUsaComparacaoEstritaNosExtremos() {
        // Gatilho SBPC6973: estritamente após 30.09.1969 e antes de 02.01.1973.
        assertFalse(SBPC6973Service.dentroPeriodoAcumulo(LocalDate.of(1969, 9, 30)));
        assertFalse(SBPC6973Service.dentroPeriodoAcumulo(LocalDate.of(1973, 1, 2)));
        assertTrue(SBPC6973Service.dentroPeriodoAcumulo(LocalDate.of(1970, 3, 31)));
        assertTrue(SBPC6973Service.isInicioPeriodo(LocalDate.of(1969, 9, 30)));
        assertTrue(SBPC6973Service.isFimPeriodo(LocalDate.of(1973, 1, 2)));
    }

    @Test void calcSbpcReconheceCodigosEstendidos() {
        // CALCSBPC varre 003,005,006,007,008,780,781,782,783.
        assertTrue(SBPC6973Service.codigoCalcSbpc("6"));
        assertTrue(SBPC6973Service.codigoCalcSbpc("008"));
        assertTrue(SBPC6973Service.codigoCalcSbpc("783"));
        assertFalse(SBPC6973Service.codigoCalcSbpc("999"));
    }
}
