package br.com.credjam.domain;

import br.com.credjam.dto.Modelo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name="HISTORICO_ATUALIZACAO")
public class HistoricoAtualizacaoEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="RESULTADO_ID") private ResultadoAtualizacaoEntity resultado;
    @Column(name="SEQUENCIA", nullable=false) private Integer sequencia;
    @Column(name="DATA_COEFICIENTE", nullable=false) private LocalDate dataCoeficiente;
    @Column(name="TAXA_ANUAL", nullable=false) private Integer taxaAnual;
    @Column(name="COEFICIENTE_APLICADO", nullable=false, precision=20, scale=12) private BigDecimal coeficienteAplicado;
    @Column(name="BASE_CALCULO", nullable=false, precision=19, scale=2) private BigDecimal baseCalculo;
    @Column(name="SALDO_ANTERIOR", nullable=false, precision=19, scale=2) private BigDecimal saldoAnterior;
    @Column(name="JAM_PERIODO", nullable=false, precision=19, scale=2) private BigDecimal jamPeriodo;
    @Column(name="SALDO_POSTERIOR", nullable=false, precision=19, scale=2) private BigDecimal saldoPosterior;
    @Column(name="REGRA_ESPECIAL") private String regraEspecial;
    @Column(name="OBSERVACAO") private String observacao;
    protected HistoricoAtualizacaoEntity() {}
    public HistoricoAtualizacaoEntity(ResultadoAtualizacaoEntity resultado, Modelo.ItemMemoria i) {
        this.resultado=resultado; this.sequencia=i.sequencia(); this.dataCoeficiente=i.dataCoeficiente();
        this.taxaAnual=i.taxaAnual(); this.coeficienteAplicado=i.coeficienteAplicado();
        this.baseCalculo=i.baseCalculo(); this.saldoAnterior=i.saldoAnterior();
        this.jamPeriodo=i.jamPeriodo(); this.saldoPosterior=i.saldoPosterior();
        this.regraEspecial=i.regraEspecial(); this.observacao=i.observacao();
    }
}
