package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Grupo;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.GrupoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import br.ufma.springextensao.service.UsuarioService;

import java.util.List;
import static br.ufma.springextensao.util.Sessao.logado;

@RestController
@RequestMapping("/api/usuario")
public class GrupoController {
    @Autowired
    GrupoService grupoService;

    @Autowired
    UsuarioService usuarioService;

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public Grupo criarGrupo(@RequestBody GrupoDTO grupo) {
        return grupoService.criar(grupo);
    }

    @PatchMapping("/aprovar/{idGrupo}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo aprovar(@PathVariable Integer idGrupo, @RequestParam Integer idDiscente, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return grupoService.aprovar(solicitante, idGrupo, idDiscente);
    }

    @PatchMapping("/rejeitar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo rejeitar(@PathVariable Integer id, @RequestParam String justificativa, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return grupoService.rejeitar(solicitante, id, justificativa);
    }

    @PatchMapping("/addmembro/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo adicionarMembro(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return grupoService.adicionarMembro(solicitante, idGrupo, idDiscente);
    }

    @PatchMapping("/removemembro/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo removerMembro(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return grupoService.removerMembro(solicitante, idGrupo, idDiscente);
    }

    @PatchMapping("/atribuircargo/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo atribuirCargo(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente,
                               @RequestParam String cargo, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return grupoService.atribuirCargo(solicitante, idDiscente, idGrupo, cargo);
    }

    @PatchMapping("/removercargo/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo removerCargo(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente,
                              @RequestParam String cargo, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return grupoService.removerCargo(solicitante, idDiscente, idGrupo, cargo);
    }

    @GetMapping("/grupo/{id}")
    public Grupo buscaId(@PathVariable Integer id) {
        return grupoService.buscaPorId(id);
    }

    @GetMapping("/lista")
    public List<Grupo> lista() {
        return grupoService.listaGrupos();
    }

    @GetMapping("/lista/membros/{id}")
    public List<Discente> listaMembros(@PathVariable Integer id) {
        return grupoService.listaGrupoMembros(id);
    }
}
