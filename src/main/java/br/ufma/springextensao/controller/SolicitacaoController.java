package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Solicitacao;
import br.ufma.springextensao.service.SolicitacaoService;
import br.ufma.springextensao.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/solicitacao")
public class SolicitacaoController {
    @Autowired
    SolicitacaoService solicitacaoService;

    @Autowired
    UsuarioService usuarioService;

    @PostMapping("/submeter")
    @ResponseStatus(HttpStatus.CREATED)
    public Solicitacao submeterNovaSolicitacao(@RequestBody SolicitacaoDTO solicitacao) {
        return solicitacaoService.submeter(solicitacao);
    }

    @GetMapping("/{id}")
    public Solicitacao buscarPorId(@PathVariable Integer id) {
        return solicitacaoService.buscarPorId(id);
    }

    @GetMapping("/discente/{id}")
    public List<Solicitacao> listarPorDiscente(@PathVariable Integer id) {
        return solicitacaoService.listarPorDiscente(id);
    }

    @GetMapping("/pendentes")
    public List<Solicitacao> listarPendentes() {
        return solicitacaoService.listarPendentes();
    }

    // fazer de aprovar, indeferir e reenviar
}
