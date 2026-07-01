package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.service.InscricaoService;
import br.ufma.springextensao.model.Inscricao;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;

import static br.ufma.springextensao.util.Sessao.logado;

@RestController
@RequestMapping("/api/inscricao")
public class InscricaoController {

    @Autowired
    InscricaoService inscricaoService;

    @Autowired
    UsuarioService usuarioService;

    @PostMapping("/inscrever")
    @ResponseStatus(HttpStatus.CREATED)
    public Inscricao inscrever(@RequestBody InscricaoDTO inscricao) {
        return inscricaoService.inscrever(inscricao);
    }

    @PatchMapping("/aprovar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Inscricao aprovar(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return inscricaoService.aprovar(id, solicitante);
    }

    @PatchMapping("/rejeitar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Inscricao rejeitar(@PathVariable Integer id, @RequestParam String justificativa, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return inscricaoService.rejeitar(id, justificativa, solicitante);
    }

    @PatchMapping("/remover/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Inscricao remover(@PathVariable Integer id, @RequestParam String justificativa, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return inscricaoService.removerDiscente(id, justificativa, solicitante);
    }

    @PatchMapping("/desistir/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Inscricao desistir(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return inscricaoService.desistir(id, solicitante);

    }

    @GetMapping("/lista/oportunidade/{id}")
    public List<Inscricao> listarPorOportunidade(@PathVariable Integer id) {
        return inscricaoService.listarPorOportunidade(id);
    }

    @GetMapping("/lista/fila-espera/{id}")
    public List <Inscricao> listarFilaEspera(@PathVariable Integer id) {
        return inscricaoService.listarFilaEspera(id);
    }

    @GetMapping("/lista/discente/{id}")
    public List <Inscricao> listarPorDiscente(@PathVariable Integer id) {
        return inscricaoService.listarPorDiscente(id);
    }

}
