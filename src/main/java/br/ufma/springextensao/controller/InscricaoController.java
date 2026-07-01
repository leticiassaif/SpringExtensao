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

    @PatchMapping("/rejeitar/{inscricaoId}")
    @ResponseStatus(HttpStatus.OK)
    public Inscricao rejeitarRemoverDiscente(@PathVariable Integer inscricaoId, @RequestParam String justificativa,
                                             @RequestBody Oportunidade oportunidade, HttpSession session) {
        Usuario solicitante = usuarioService.buscarPorId((Integer) session.getAttribute("IdUsuarioLogado"));
        if (solicitante == null) {
            throw new SecurityException("Usuário não está logado!");
        }
        return inscricaoService.rejeitarRemoverDiscente(inscricaoId, justificativa, oportunidade, solicitante);
    }

    @PatchMapping("/desistir/{inscricaoId}")
    public Inscricao desistir(@PathVariable Integer inscricaoId, @RequestBody Oportunidade oportunidade,
                              HttpSession session) {
        Usuario solicitante = usuarioService.buscarPorId((Integer) session.getAttribute("IdUsuarioLogado"));
        if (solicitante == null) {
            throw new SecurityException("Usuário não está logado!");
        }
        return inscricaoService.desistir(inscricaoId, oportunidade, solicitante);

    }

    @GetMapping("/oportunidade/{id}")
    public List<Inscricao> listarPorOportunidade(@PathVariable Integer id) {
        return inscricaoService.listarPorOportunidade(oportunidade);
    }

    @GetMapping("/oportunidade/{id}/fila-espera")
    public List <Inscricao> listarFilaEspera(@PathVariable Integer id) {
        return inscricaoService.listarFilaEspera(oportunidade);
    }

    @GetMapping("/discente/{id}")
    public List <Inscricao> listarPorDiscente(@PathVariable Integer id) {
        return inscricaoService.listarPorDiscente(discente);
    }

}
