package br.com.credjam.service;

import br.com.credjam.domain.CoeficienteJamEntity;
import br.com.credjam.domain.ContaAtualizacaoEntity;
import br.com.credjam.domain.HistoricoAtualizacaoEntity;
import br.com.credjam.domain.ResultadoAtualizacaoEntity;
import br.com.credjam.dto.Modelo;
import br.com.credjam.exception.ErroNegocioException;
import br.com.credjam.infrastructure.repository.CoeficienteJamRepository;
import br.com.credjam.infrastructure.repository.ContaAtualizacaoRepository;
import br.com.credjam.infrastructure.repository.HistoricoAtualizacaoRepository;
import br.com.credjam.infrastructure.repository.ResultadoAtualizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AtualizacaoContaService {
    private final ValidacaoAtualizacaoService validacao;
    private final CoeficienteJamRepository coeficientes;
    private final TaxaJurosService taxas;
    private final JamService jam;
    private final ContaAtualizacaoRepository contas;
    private final ResultadoAtualizacaoRepository resultados;
    private final HistoricoAtualizacaoRepository historicos;
    private final CalcSbpcService calcSbpc;
    private final ConversaoMonetariaService conversoes = new ConversaoMonetariaService();
    private final ComplementoHistoricoService complemento1976 = new ComplementoHistoricoService();
    private final JAM6973Service jam6973 = new JAM6973Service();

    public AtualizacaoContaService(ValidacaoAtualizacaoService validacao, CoeficienteJamRepository coeficientes,
      TaxaJurosService taxas, JamService jam, ContaAtualizacaoRepository contas,
      ResultadoAtualizacaoRepository resultados, HistoricoAtualizacaoRepository historicos,
      CalcSbpcService calcSbpc) {
        this.validacao=validacao; this.coeficientes=coeficientes; this.taxas=taxas; this.jam=jam;
        this.contas=contas; this.resultados=resultados; this.historicos=historicos;
        this.calcSbpc=calcSbpc;
    }

    @Transactional
    public Modelo.ResultadoResponse atualizar(Modelo.AtualizacaoRequest r) {
        validacao.validar(r);
        var conta=ContaAtualizacaoEntity.nova(r);
        conta.setStatus("RECEBIDA");
        conta=contas.save(conta);

        BigDecimal saldo=r.saldoInicial().setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal saldoInicial=saldo;
        // Fator acumulado das conversões monetárias aplicadas no período. Usado para
        // trazer o saldo inicial à MESMA escala do saldo final, como no fechamento do
        // COBOL (JAM = saldo final - total de depósitos, todos na mesma moeda).
        BigDecimal fatorConversaoAcumulado=BigDecimal.ONE;
        BigDecimal jamAcumulado=BigDecimal.ZERO.setScale(2);
        BigDecimal saldoPosJam = moeda(r.saldoPosJamInicial());
        BigDecimal totalDepositos = r.totalDepositosInicial();
        BigDecimal jamInformado = r.jamInformadoInicial();
        BigDecimal saldoComplementar = moeda(r.saldoComplementarInicial());
        BigDecimal sbpcPendente = BigDecimal.ZERO.setScale(2);
        SBPC6973Service sbpc6973 = new SBPC6973Service();
        // FLAG-CTA-JUDICIAL do COBOL: conta judicial quando COD-TIPO-CONTA = 3.
        boolean contaJudicial = "3".equals(r.tipoConta());
        List<String> avisosSbpc = new ArrayList<>();
        if (r.lancamentosHistoricos() != null) {
            for (Modelo.LancamentoHistoricoRequest lancamento : r.lancamentosHistoricos()) {
                if (lancamento == null) continue;
                // CALCSBPC: a data de crédito do JAM é derivada do FGCRB560, não informada.
                CalcSbpcService.Resultado calc = calcSbpc.derivar(
                    lancamento.codigoLancamento(), lancamento.dataLancamento(),
                    lancamento.dataCompetencia(), contaJudicial);
                if (calc.derivada()) {
                    sbpc6973.acumular(calc.dataCreditoJam(), lancamento.codigoLancamento(),
                        lancamento.valorDeposito(), lancamento.valorJam());
                } else if (!calc.calendarioDisponivel()) {
                    avisosSbpc.add("FGCRB560 não carregado: não foi possível derivar a data de crédito JAM do lançamento "
                        + lancamento.codigoLancamento() + " de " + lancamento.dataLancamento()
                        + "; o lançamento não entrou no acúmulo SBPC6973.");
                }
            }
        }
        LocalDate referencia=r.dataBase();
        LocalDate ultimaData=null;
        int seq=0;
        boolean demonstracao=false;
        String versao="SEM_DADOS";
        List<Modelo.ItemMemoria> memoria=new ArrayList<>();
        List<String> avisos=new ArrayList<>();

        while (true) {
            List<CoeficienteJamEntity> proximos=coeficientes.proximos(referencia);
            CoeficienteJamEntity proximo=proximos.stream()
                .filter(c -> c.getDataAtualizacao().isBefore(r.dataLimite()))
                .findFirst().orElse(null);
            if (proximo==null) break;

            var estadoConversao = new ConversaoMonetariaService.Estado(saldo, saldoPosJam, jamAcumulado,
                totalDepositos, jamInformado, saldoComplementar);
            var resultadoConversao = conversoes.aplicarTransicoes(referencia, proximo.getDataAtualizacao(), estadoConversao);
            for (var evento : resultadoConversao.eventos()) {
                if (evento.fator().compareTo(BigDecimal.ONE) > 0) {
                    fatorConversaoAcumulado = fatorConversaoAcumulado.multiply(evento.fator());
                }
            }
            estadoConversao = resultadoConversao.estado();
            saldo = estadoConversao.saldo();
            saldoPosJam = estadoConversao.saldoPosJam();
            jamAcumulado = estadoConversao.jamAcumulado();
            totalDepositos = estadoConversao.totalDepositos();
            jamInformado = estadoConversao.jamInformado();
            saldoComplementar = estadoConversao.saldoComplementar();

            var taxa=taxas.selecionar(proximo.getDataAtualizacao(), r.dataOpcao(), r.codigoAfastamento(),
                r.dataAfastamento(), proximo.getDataAtualizacao());
            BigDecimal coef=proximo.coeficiente(taxa.taxa());
            if (coef==null || coef.signum()<0)
                throw new ErroNegocioException("COEFICIENTE_INVALIDO", "Coeficiente ausente ou negativo.");
            LocalDate dataCoef = proximo.getDataAtualizacao();
            BigDecimal antes=saldo;
            String regraEspecial = null;
            StringBuilder observacoes = new StringBuilder(taxa.observacao())
                .append(" Dados de coeficiente: ").append(proximo.getFonteDado()).append(".");
            if (SBPC6973Service.dataHistorica(dataCoef)) {
                BigDecimal saldoAntesReincorporacao = saldo;
                saldo = jam6973.reincorporar(dataCoef, saldo, sbpc6973);
                if (saldo.compareTo(saldoAntesReincorporacao) != 0) {
                    regraEspecial = "JAM6973";
                    observacoes.append(" JAM-6973: acumulador da data reincorporado antes do cálculo do coeficiente.");
                }
            }
            BigDecimal base=saldo;
            BigDecimal credito=saldo.signum()>0 ? jam.calcular(base, coef) : BigDecimal.ZERO.setScale(2);

            // No intervalo especial, o JAM do período fica pendente até a reincorporação histórica.
            // Gatilho SBPC6973 (COBOL): DTA-CREDITO-JAM MAIOR QUE 30.09.1969 E MENOR QUE 02.01.1973
            // (comparação estrita nos dois extremos). O fim do período (02.01.1973) é tratado à parte.
            if (SBPC6973Service.dentroPeriodoAcumulo(dataCoef)) {
                saldo = saldo.add(sbpcPendente).setScale(2, java.math.RoundingMode.HALF_UP);
                sbpcPendente = credito;
                regraEspecial = "SBPC6973_PENDENTE";
                observacoes.append(" SBPC6973: saldo pendente reincorporado e JAM do período mantido separado.");
            } else if (SBPC6973Service.isFimPeriodo(dataCoef)) {
                saldo = saldo.add(sbpcPendente).add(saldoPosJam).add(credito)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
                sbpcPendente = BigDecimal.ZERO.setScale(2);
                saldoPosJam = BigDecimal.ZERO.setScale(2);
                regraEspecial = "FECHAMENTO_SBPC6973";
                observacoes.append(" Fechamento do período histórico em 02/01/1973.");
            } else {
                saldo = saldo.add(credito).setScale(2, java.math.RoundingMode.HALF_UP);
            }
            jamAcumulado=jamAcumulado.add(credito).setScale(2, java.math.RoundingMode.HALF_UP);

            var complemento = complemento1976.aplicar(dataCoef, saldo, saldoComplementar);
            if (complemento.aplicado()) {
                saldo = complemento.saldo();
                regraEspecial = regraEspecial == null ? "COMPLEMENTO_1976_11_3" : regraEspecial + "+COMPLEMENTO_1976_11_3";
                observacoes.append(" Complemento de 11,3% aplicado somente sobre VAL-SALDO-COMPLEMENTAR: ")
                    .append(complemento.valorComplemento()).append(".");
            }
            // COBOL: (DTA-PROXIMO-JAM MENOR QUE 30.09.1969) OU (MAIOR QUE 02.01.1973)
            // reincorpora o saldo pós-JAM; os extremos do período NÃO entram aqui.
            if (dataCoef.isBefore(LocalDate.of(1969, 9, 30)) || dataCoef.isAfter(LocalDate.of(1973, 1, 2))) {
                if (saldoPosJam.signum() != 0) {
                    saldo = saldo.add(saldoPosJam).setScale(2, java.math.RoundingMode.HALF_UP);
                    observacoes.append(" Saldo pós-JAM reincorporado fora do intervalo SBPC6973.");
                    regraEspecial = regraEspecial == null ? "REINCORPORACAO_POS_JAM" : regraEspecial + "+REINCORPORACAO_POS_JAM";
                }
                saldoPosJam = BigDecimal.ZERO.setScale(2);
            }
            for (var evento : resultadoConversao.eventos()) {
                observacoes.append(" Conversão ").append(evento.codigo()).append(" em ")
                    .append(evento.data()).append("; fator ").append(evento.fator()).append(". ");
                regraEspecial = regraEspecial == null ? evento.codigo() : regraEspecial + "+" + evento.codigo();
            }
            seq++;
            memoria.add(new Modelo.ItemMemoria(seq, dataCoef, taxa.taxa(), coef,
                base, credito, antes, saldo, regraEspecial, observacoes.toString()));
            demonstracao |= !"OFICIAL".equalsIgnoreCase(proximo.getFonteDado());
            versao=proximo.getVersaoCarga();
            ultimaData=proximo.getDataAtualizacao();
            referencia=proximo.getDataAtualizacao();
        }

        if (seq==0) {
            throw new ErroNegocioException("COEFICIENTES_AUSENTES",
                "Nenhum coeficiente elegível foi encontrado entre a data-base e a data de corte. A execução não foi processada.");
        }
        if (memoria.stream().anyMatch(i -> i.observacao()!=null && i.observacao().contains("DEMONSTRACAO")))
            demonstracao=true;
        if (demonstracao) avisos.add("Resultado demonstrativo: foram utilizados dados fictícios.");
        if (seq>0 && !r.dataLimite().equals(ultimaData) && coeficientes.proximos(referencia).stream().noneMatch(c -> c.getDataAtualizacao().isBefore(r.dataLimite()))) {
            avisos.add("O processamento terminou no último coeficiente disponível antes da data de corte.");
        }
        // Fechamento coerente em escala (fiel ao COBOL): o saldo inicial exibido é
        // trazido à mesma moeda do saldo final, aplicando o fator acumulado das
        // conversões. Assim evita-se somar valores pré e pós-conversão (que gerava
        // "saldo inicial + JAM" incompatível com o saldo final).
        BigDecimal saldoInicialNaEscalaFinal = fatorConversaoAcumulado.compareTo(BigDecimal.ONE) > 0
            ? saldoInicial.divide(fatorConversaoAcumulado, 2, java.math.RoundingMode.HALF_UP)
            : saldoInicial;
        // No fluxo do legado, o JAM total de fechamento é VAL-SALDO - VAL-TOT-DEP, com
        // ambos na mesma escala. Quando o total de depósitos não é informado, usa-se o
        // saldo inicial (na escala final) como base equivalente, de modo que
        // saldoInicialNaEscalaFinal + jamTotal = saldoFinal.
        BigDecimal baseDepositos = totalDepositos != null
            ? totalDepositos
            : saldoInicialNaEscalaFinal;
        BigDecimal jamTotal = saldo.subtract(baseDepositos).setScale(2, java.math.RoundingMode.HALF_UP);
        if (r.totalDepositosInicial()==null)
            avisos.add("Total de depósitos inicial não informado; JAM total apurado como saldo final menos o saldo inicial trazido à mesma escala monetária, não como fechamento completo do legado.");
        if (fatorConversaoAcumulado.compareTo(BigDecimal.ONE) > 0)
            avisos.add("Conversões monetárias aplicadas no período (fator acumulado " + fatorConversaoAcumulado.stripTrailingZeros().toPlainString()
                + "); o saldo inicial exibido no consolidado foi convertido para a moeda da data de corte para manter a coerência do fechamento.");
        if (r.dataBase().isBefore(LocalDate.of(1973, 1, 2)) && r.dataLimite().isAfter(LocalDate.of(1969, 9, 29))) {
            avisos.add("A paridade integral de SBPC6973/JAM-6973 depende dos lançamentos históricos e do calendário oficial FGCRB560. Sem esses dados, o resultado histórico não deve ser considerado centavo-a-centavo equivalente ao COBOL.");
        }
        boolean acumuladorHistoricoPendente = sbpc6973.fotografia().values().stream().anyMatch(v -> v.signum() != 0);
        if (sbpcPendente.signum() != 0) {
            avisos.add("Existe JAM de período SBPC6973 ainda pendente na data de corte; o fechamento depende da próxima data histórica de crédito JAM.");
        }
        if (acumuladorHistoricoPendente) {
            avisos.add("Há acumuladores SBPC6973 não reincorporados: confira se a carga FGCRB550 contém todas as datas históricas de JAM exigidas pelo legado.");
        }
        if (r.lancamentosHistoricos() == null || r.lancamentosHistoricos().isEmpty()) {
            avisos.add("Nenhum lançamento individual foi fornecido para SBPC6973; a rotina só processou os saldos consolidados e os coeficientes disponíveis.");
        }
        avisos.addAll(avisosSbpc);
        var resultado=new ResultadoAtualizacaoEntity(conta, saldo, jamTotal, ultimaData, demonstracao, versao);
        resultado=resultados.save(resultado);
        for (var item: memoria) historicos.save(new HistoricoAtualizacaoEntity(resultado,item));
        conta.setStatus("PROCESSADA");
        return new Modelo.ResultadoResponse(resultado.getId(),"PROCESSADA",r.origem(),r.dataBase(),r.dataLimite(),
            saldoInicialNaEscalaFinal,saldo,jamTotal,demonstracao,avisos,memoria);
    }

    private static BigDecimal moeda(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
