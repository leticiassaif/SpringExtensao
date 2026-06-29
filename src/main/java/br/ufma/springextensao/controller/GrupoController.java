package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.model.Grupo;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.GrupoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GrupoController {
    @Autowired
    GrupoService grupoService;

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public Grupo criarGrupo(@RequestBody GrupoDTO grupo) {
        return grupoService.criar(grupo, );
    }

    @PatchMapping("/aprovar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo aprovar(@PathVariable Integer idGrupo, @RequestParam Integer idDiscente) {
        return grupoService.aprovar(, idGrupo, idDiscente);
    }

    @PatchMapping("/rejeitar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo rejeitar(@PathVariable Integer id, @RequestParam String justificativa) {
        return grupoService.rejeitar(, id, justificativa);
    }

    @PatchMapping("/addmembro/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo adicionarMembro(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente) {
        return grupoService.adicionarMembro(, idGrupo, idDiscente);
    }

    @PatchMapping("/removemembro/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo removerMembro(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente) {
        return grupoService.removerMembro(, idGrupo, idDiscente);
    }

    @PatchMapping("/atribuircargo/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo atribuirCargo(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente,
                               @RequestParam String cargo) {
        return grupoService.atribuirCargo(, idDiscente, idGrupo, cargo);
    }

    @PatchMapping("/removercargo/{idGrupo}/{idDiscente}")
    @ResponseStatus(HttpStatus.OK)
    public Grupo removerCargo(@PathVariable Integer idGrupo, @PathVariable Integer idDiscente,
                               @RequestParam String cargo) {
        return grupoService.removerCargo(, idDiscente, idGrupo, cargo);
    }

    @GetMapping("/id/{id}")
    public Grupo buscaId(@PathVariable Integer id) {
        return grupoService.buscaPorId(id);
    }

    @GetMapping("/lista")
    public List<Grupo> lista() {
        return grupoService.listaGrupos();
    }

    @GetMapping("/lista/membros/{id}")
    public List<Usuario> listaMembros(@PathVariable Integer id) {
        return grupoService.listaGrupoMembros(id);
    }
}
