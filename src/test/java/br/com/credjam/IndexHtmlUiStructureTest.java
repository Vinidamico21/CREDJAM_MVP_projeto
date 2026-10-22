package br.com.credjam;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class IndexHtmlUiStructureTest {

    private static final Path INDEX = Path.of("src/main/resources/static/index.html");

    @Test
    void deveConterConsolidadoComDetalhesFechadosEPaginacao() throws IOException {
        String html = Files.readString(INDEX, StandardCharsets.UTF_8);

        assertTrue(html.contains("id=\"detalhesToggle\""), "Botao de detalhes deve existir.");
        assertTrue(html.contains("id=\"detalhesSection\" hidden"), "Detalhes devem iniciar fechados.");
        assertTrue(html.contains("const PAGE_SIZE=20;"), "Paginacao deve usar 20 registros por pagina.");
        assertTrue(html.contains("renderPagina()"), "Renderizacao paginada deve existir.");
        assertTrue(html.contains("slice(inicio,fim)"), "Tabela deve renderizar apenas a fatia da pagina atual.");
        assertTrue(html.contains("paginaAtual=1;"), "Nova execucao deve reiniciar na primeira pagina.");
        assertTrue(html.contains("JAM acumulado calculado"), "Consolidado deve destacar JAM calculado.");
        assertTrue(html.contains("Períodos processados"), "Consolidado deve informar quantidade de periodos.");
    }

    @Test
    void deveTerPreenchimentoELimpezaSeparadosPorGrupo() throws IOException {
        String html = Files.readString(INDEX, StandardCharsets.UTF_8);

        assertTrue(html.contains("id=\"preencherObrig\""), "Deve existir botao de preencher obrigatorios.");
        assertTrue(html.contains("id=\"limparObrig\""), "Deve existir botao de limpar obrigatorios.");
        assertTrue(html.contains("id=\"preencherOpc\""), "Deve existir botao de preencher opcionais.");
        assertTrue(html.contains("id=\"limparOpc\""), "Deve existir botao de limpar opcionais.");
        assertTrue(html.contains("function preencherObrigatorios()"), "Deve existir rotina de preenchimento dos obrigatorios.");
        assertTrue(html.contains("function preencherOpcionais()"), "Deve existir rotina de preenchimento dos opcionais.");
    }
}

