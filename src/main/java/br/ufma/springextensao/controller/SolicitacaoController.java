package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.model.Solicitacao;
import br.ufma.springextensao.service.SolicitacaoService;
import br.ufma.springextensao.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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
    public Solicitacao submeter(@RequestBody SolicitacaoDTO solicitacao) {
        return solicitacaoService.submeter(solicitacao);
    }

    @PatchMapping("/aprovar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Solicitacao aprovar(@PathVariable Integer id) {
        return solicitacaoService.aprovar(, id);
    }

    @PatchMapping("/indeferir/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Solicitacao indeferir(@PathVariable Integer id, @RequestParam String parecer) {
        return solicitacaoService.indeferir(, id, parecer);
    }

    @PatchMapping("/reenviar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Solicitacao reenviar(@PathVariable Integer id) {
        return solicitacaoService.reenviar(id);
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
}
