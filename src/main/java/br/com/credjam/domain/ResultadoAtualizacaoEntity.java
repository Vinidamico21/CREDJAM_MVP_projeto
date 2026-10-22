package br.com.credjam.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="RESULTADO_ATUALIZACAO")
public class ResultadoAtualizacaoEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="CONTA_ATUALIZACAO_ID") private ContaAtualizacaoEntity conta;
    @Column(name="SALDO_FINAL", nullable=false, precision=19, scale=2) private BigDecimal saldoFinal;
    @Column(name="JAM_TOTAL", nullable=false, precision=19, scale=2) private BigDecimal jamTotal;
    @Column(name="DATA_FINAL_PROCESSADA") private LocalDate dataFinalProcessada;
    @Column(name="DADOS_DEMONSTRACAO", nullable=false) private boolean dadosDemonstracao;
    @Column(name="VERSAO_CARGA") private String versaoCarga;
    @Column(name="CRIADO_EM", nullable=false) private LocalDateTime criadoEm;
    protected ResultadoAtualizacaoEntity() {}
    public ResultadoAtualizacaoEntity(ContaAtualizacaoEntity conta, BigDecimal saldoFinal, BigDecimal jamTotal,
                                      LocalDate dataFinalProcessada, boolean demo, String versao) {
        this.conta=conta; this.saldoFinal=saldoFinal; this.jamTotal=jamTotal;
        this.dataFinalProcessada=dataFinalProcessada; this.dadosDemonstracao=demo;
        this.versaoCarga=versao; this.criadoEm=LocalDateTime.now();
    }
    public Long getId(){ return id; }
}
