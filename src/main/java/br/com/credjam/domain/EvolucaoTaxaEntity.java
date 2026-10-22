package br.com.credjam.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity @Table(name="FGCRB555")
public class EvolucaoTaxaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="DATA_INICIO", nullable=false) private LocalDate dataInicio;
    @Column(name="DATA_FIM") private LocalDate dataFim;
    @Column(name="TAXA_ANUAL", nullable=false) private Integer taxaAnual;
    @Column(name="FONTE_DADO", nullable=false) private String fonteDado;
    @Column(name="VERSAO_CARGA", nullable=false) private String versaoCarga;
    protected EvolucaoTaxaEntity() {}
    public boolean vigente(LocalDate data) {
        return !data.isBefore(dataInicio) && (dataFim == null || data.isBefore(dataFim));
    }
    public Integer getTaxaAnual(){ return taxaAnual; }
    public String getFonteDado(){ return fonteDado; }
    public String getVersaoCarga(){ return versaoCarga; }
}
