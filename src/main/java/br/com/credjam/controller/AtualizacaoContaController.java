package br.com.credjam.controller;

import br.com.credjam.dto.Modelo;
import br.com.credjam.service.AtualizacaoContaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AtualizacaoContaController {
    private final AtualizacaoContaService service;
    public AtualizacaoContaController(AtualizacaoContaService service){ this.service=service; }

    @PostMapping("/v1/atualizacoes-conta")
    public Modelo.ResultadoResponse atualizarV1(@RequestBody Modelo.AtualizacaoRequest request) {
        return service.atualizar(request);
    }

    @PostMapping("/atualizacoes-conta")
    public Modelo.ResultadoResponse atualizarLegado(@RequestBody Modelo.AtualizacaoRequest request) {
        return service.atualizar(request);
    }
}
