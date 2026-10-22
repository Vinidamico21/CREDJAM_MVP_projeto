package br.com.credjam.service;

import br.com.credjam.dto.Modelo;
import br.com.credjam.exception.ErroNegocioException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Set;

@Service
public class ValidacaoAtualizacaoService {
    private static final Set<String> ORIGENS = Set.of("FGTS_DIGITAL", "PGFN", "DEMONSTRACAO");

    public void validar(Modelo.AtualizacaoRequest r) {
        if (r == null) throw new ErroNegocioException("ENTRADA_AUSENTE", "Informe os dados da atualização.");
        if (r.origem()==null || !ORIGENS.contains(r.origem()))
            throw new ErroNegocioException("ORIGEM_INVALIDA", "Origem deve ser FGTS_DIGITAL, PGFN ou DEMONSTRACAO.");
        if (r.dataBase()==null || r.dataLimite()==null || r.saldoInicial()==null)
            throw new ErroNegocioException("CAMPO_OBRIGATORIO", "Data-base, data de corte e saldo inicial são obrigatórios.");
        if (r.dataLimite().isBefore(r.dataBase()))
            throw new ErroNegocioException("DATA_LIMITE_ANTERIOR", "A data de corte não pode ser anterior à data-base.");
        if (r.dataLimite().isAfter(LocalDate.now()))
            throw new ErroNegocioException("DATA_FUTURA", "A data de corte não pode ser futura.");
        if (r.saldoInicial().signum()<0)
            throw new ErroNegocioException("SALDO_NEGATIVO", "O saldo inicial não pode ser negativo neste cenário demonstrativo.");
        if (r.lancamentosHistoricos() != null) {
            for (Modelo.LancamentoHistoricoRequest l : r.lancamentosHistoricos()) {
                if (l == null || l.dataLancamento() == null || l.codigoLancamento() == null
                    || l.valorDeposito() == null || l.valorJam() == null) {
                    throw new ErroNegocioException("LANCAMENTO_HISTORICO_INVALIDO",
                        "Cada lançamento histórico deve informar dataLancamento, codigoLancamento, valorDeposito e valorJam.");
                }
                if (l.dataLancamento().isAfter(r.dataLimite())) {
                    throw new ErroNegocioException("LANCAMENTO_FORA_DO_CORTE",
                        "A data de lançamento de um lançamento histórico não pode ser posterior à data de corte.");
                }
            }
        }
    }
}
