package br.com.credjam.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name="FGCRB550")
public class CoeficienteJamEntity {
    @Id @Column(name="DATA_ATUALIZACAO") private LocalDate dataAtualizacao;
    @Column(name="COEF_3", nullable=false, precision=20, scale=12) private BigDecimal coef3;
    @Column(name="COEF_4", nullable=false, precision=20, scale=12) private BigDecimal coef4;
    @Column(name="COEF_5", nullable=false, precision=20, scale=12) private BigDecimal coef5;
    @Column(name="COEF_6", nullable=false, precision=20, scale=12) private BigDecimal coef6;
    @Column(name="FONTE_DADO", nullable=false) private String fonteDado;
    @Column(name="VERSAO_CARGA", nullable=false) private String versaoCarga;
    protected CoeficienteJamEntity() {}
    public LocalDate getDataAtualizacao(){ return dataAtualizacao; }
    public BigDecimal coeficiente(int taxa) {
        return switch (taxa) {
            case 3 -> coef3; case 4 -> coef4; case 5 -> coef5; case 6 -> coef6;
            default -> throw new IllegalArgumentException("Taxa não suportada: " + taxa);
        };
    }
    public String getFonteDado(){ return fonteDado; }
    public String getVersaoCarga(){ return versaoCarga; }
}
