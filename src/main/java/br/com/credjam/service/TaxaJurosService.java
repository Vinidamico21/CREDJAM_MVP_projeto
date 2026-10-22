package br.com.credjam.service;

import br.com.credjam.domain.EvolucaoTaxaEntity;
import br.com.credjam.exception.ErroNegocioException;
import br.com.credjam.infrastructure.repository.EvolucaoTaxaRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class TaxaJurosService {
    private static final LocalDate MARCO_TAXA_3 = LocalDate.of(1971, 9, 22);
    private static final Set<String> AFASTAMENTOS_COM_REGRA_ESPECIAL = Set.of(
        "", "N", "O1", "P1", "Q1", "R", "S", "U1", "I", "X", "Y", "Z"
    );
    private final EvolucaoTaxaRepository repository;

    public TaxaJurosService(EvolucaoTaxaRepository repository) { this.repository = repository; }

    /**
     * Seleção baseada na rotina VERTAXA documentada no FGCD0080.
     * Para afastamentos especiais, compara a próxima data de JAM com a data de afastamento;
     * a taxa 3 só é forçada quando a data de opção posterior ao marco também se aplica.
     */
    public TaxaSelecionada selecionar(LocalDate dataAtualizacao, LocalDate dataOpcao) {
        return selecionar(dataAtualizacao, dataOpcao, null, null, dataAtualizacao);
    }

    public TaxaSelecionada selecionar(LocalDate dataAtualizacao, LocalDate dataOpcao,
                                      String codigoAfastamento, LocalDate dataAfastamento,
                                      LocalDate dataProximoJam) {
        String codigo = normalizarAfastamento(codigoAfastamento);
        // O COBOL inicializa TX-JUROS com 3%; somente o ramo de afastamentos listados
        // recalcula a taxa usando a data de opção, DTA-AUX e a evolução da FGCRB555.
        if (!AFASTAMENTOS_COM_REGRA_ESPECIAL.contains(codigo)) {
            return new TaxaSelecionada(3, "VERTAXA: taxa padrão 3%; código de afastamento fora do ramo de evolução de taxas.");
        }
        if (dataOpcao != null && dataOpcao.isAfter(MARCO_TAXA_3)) {
            return new TaxaSelecionada(3, "VERTAXA: opção posterior a 22/09/1971; taxa 3%.");
        }

        LocalDate dataAux = dataProximoJam == null ? dataAtualizacao : dataProximoJam;
        if (dataAfastamento != null && dataAfastamento.isBefore(dataAux)) dataAux = dataAfastamento;

        LocalDate dataConsulta = dataAux;
        List<EvolucaoTaxaEntity> vigentes = repository.findAll().stream()
            .filter(e -> e.vigente(dataConsulta)).toList();
        if (vigentes.size() != 1) {
            throw new ErroNegocioException("VIGENCIA_TAXA_INVALIDA",
                vigentes.isEmpty() ? "Não existe taxa vigente na FGCRB555 para a data usada pela VERTAXA."
                    : "Há mais de uma taxa vigente na FGCRB555 para a data usada pela VERTAXA.");
        }
        EvolucaoTaxaEntity e = vigentes.get(0);
        return new TaxaSelecionada(e.getTaxaAnual(), "VERTAXA: taxa " + e.getTaxaAnual()
            + "% obtida da FGCRB555 para DTA-AUX=" + dataConsulta + ".");
    }

    private static String normalizarAfastamento(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
    }

    public record TaxaSelecionada(int taxa, String observacao) {}
}
