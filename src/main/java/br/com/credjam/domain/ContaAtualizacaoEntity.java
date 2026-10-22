package br.com.credjam.domain;

import br.com.credjam.dto.Modelo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CONTA_ATUALIZACAO")
public class ContaAtualizacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ORIGEM", nullable = false)
    private String origem;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "DATA_BASE", nullable = false)
    private LocalDate dataBase;

    @Column(name = "DATA_LIMITE", nullable = false)
    private LocalDate dataLimite;

    @Column(name = "SALDO_INICIAL", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "TOTAL_DEPOSITOS_INICIAL", precision = 19, scale = 2)
    private BigDecimal totalDepositosInicial;

    @Column(name = "JAM_INFORMADO_INICIAL", precision = 19, scale = 2)
    private BigDecimal jamInformadoInicial;

    @Column(name = "SALDO_POS_JAM_INICIAL", precision = 19, scale = 2)
    private BigDecimal saldoPosJamInicial;

    @Column(name = "SALDO_COMPLEMENTAR_INICIAL", precision = 19, scale = 2)
    private BigDecimal saldoComplementarInicial;

    @Lob
    @Column(name = "PARAMETROS_JSON")
    private String parametrosJson;

    @Column(name = "CRIADO_EM", nullable = false)
    private LocalDateTime criadoEm;

    protected ContaAtualizacaoEntity() {
    }

    public static ContaAtualizacaoEntity nova(Modelo.AtualizacaoRequest r) {
        var e = new ContaAtualizacaoEntity();

        e.origem = r.origem();
        e.status = "RECEBIDA";
        e.dataBase = r.dataBase();
        e.dataLimite = r.dataLimite();
        e.saldoInicial = r.saldoInicial();
        e.totalDepositosInicial = r.totalDepositosInicial();
        e.jamInformadoInicial = r.jamInformadoInicial();
        e.saldoPosJamInicial = r.saldoPosJamInicial();
        e.saldoComplementarInicial = r.saldoComplementarInicial();
        e.criadoEm = LocalDateTime.now();

        e.parametrosJson = "{\"dataOpcao\":\"" + r.dataOpcao()
                + "\",\"tipoConta\":\"" + r.tipoConta()
                + "\",\"codigoAfastamento\":\"" + r.codigoAfastamento()
                + "\",\"dataAfastamento\":\"" + r.dataAfastamento()
                + "\",\"dadosHistoricosCompletos\":"
                + r.dadosHistoricosCompletos() + "}";

        return e;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getOrigem() {
        return origem;
    }

    public LocalDate getDataBase() {
        return dataBase;
    }

    public LocalDate getDataLimite() {
        return dataLimite;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public BigDecimal getTotalDepositosInicial() {
        return totalDepositosInicial;
    }

    public BigDecimal getJamInformadoInicial() {
        return jamInformadoInicial;
    }

    public BigDecimal getSaldoPosJamInicial() {
        return saldoPosJamInicial;
    }

    public BigDecimal getSaldoComplementarInicial() {
        return saldoComplementarInicial;
    }
}
