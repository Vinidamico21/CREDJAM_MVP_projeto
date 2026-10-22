package br.com.credjam.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class JamService {
    private static final int ESCALA_MOEDA = 2;
    public BigDecimal calcular(BigDecimal base, BigDecimal coeficiente) {
        // Política demonstrativa; equivalência de arredondamento com COBOL ainda precisa ser confirmada.
        return base.multiply(coeficiente).setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);
    }
}
