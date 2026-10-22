package br.com.credjam.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Espelha o arquivo FGCRB560 do FGCD0080 (calendário/controle do SBPC).
 * Cada linha associa uma data de SBPC / recolhimento a uma DATA-CREDITO-JAM,
 * usada pela rotina CALCSBPC para determinar em que data histórica o JAM de
 * um lançamento deve ser creditado.
 */
@Entity @Table(name="FGCRB560")
public class CalendarioSbpcEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="DATA_SBPC") private LocalDate dataSbpc;
    @Column(name="DATA_RECOLHIMENTO") private LocalDate dataRecolhimento;
    @Column(name="DATA_CREDITO_JAM") private LocalDate dataCreditoJam;
    @Column(name="CODIGO_LANCAMENTO") private String codigoLancamento;
    @Column(name="TIPO_CONTA") private String tipoConta;
    @Column(name="FONTE_DADO", nullable=false) private String fonteDado;
    @Column(name="VERSAO_CARGA", nullable=false) private String versaoCarga;

    protected CalendarioSbpcEntity() {}

    /** Fábrica usada por cargas/testes para montar uma linha do FGCRB560. */
    public static CalendarioSbpcEntity de(LocalDate dataSbpc, LocalDate dataRecolhimento,
                                          LocalDate dataCreditoJam, String codigoLancamento,
                                          String tipoConta, String fonteDado, String versaoCarga) {
        CalendarioSbpcEntity e = new CalendarioSbpcEntity();
        e.dataSbpc = dataSbpc;
        e.dataRecolhimento = dataRecolhimento;
        e.dataCreditoJam = dataCreditoJam;
        e.codigoLancamento = codigoLancamento;
        e.tipoConta = tipoConta;
        e.fonteDado = fonteDado;
        e.versaoCarga = versaoCarga;
        return e;
    }

    public Long getId() { return id; }
    public LocalDate getDataSbpc() { return dataSbpc; }
    public LocalDate getDataRecolhimento() { return dataRecolhimento; }
    public LocalDate getDataCreditoJam() { return dataCreditoJam; }
    public String getCodigoLancamento() { return codigoLancamento; }
    public String getTipoConta() { return tipoConta; }
    public String getFonteDado() { return fonteDado; }
    public String getVersaoCarga() { return versaoCarga; }
}
