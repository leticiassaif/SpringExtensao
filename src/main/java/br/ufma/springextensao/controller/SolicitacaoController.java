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

    // tirar duvida
    @PostMapping
    public ResponseEntity<Solicitacao> submeter(@RequestBody SolicitacaoDTO dto) {
        Solicitacao solicitacao;
        Discente discente = (Discente) usuarioService.buscarPorId(dto.getIdDiscente());
        solicitacao = Solicitacao.builder().
                descricao(dto.getDescricao()).
                dataSolicitacao(LocalDate.parse(dto.getDataSolicitacao())).
                discente(discente).
                build();
        try {
            Solicitacao salvo = solicitacaoService.submeter();
        } catch () {}
    }
}
