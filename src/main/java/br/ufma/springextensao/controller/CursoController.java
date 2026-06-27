package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.CursoDTO;
import br.ufma.springextensao.model.Curso;
import br.ufma.springextensao.service.CursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/curso")
public class CursoController {
    @Autowired
    private CursoService cursoService;

    @PostMapping("/cadastar")
    @ResponseStatus(HttpStatus.CREATED)
    public Curso cadastrarCurso(@RequestBody CursoDTO curso) {
        return cursoService.cadastrarCurso(, curso);
    }

    @GetMapping("/busca/{id}")
    public Curso buscarCurso(@PathVariable Integer id) {
        return cursoService.buscaPorId(id);
    }

    @GetMapping("/busca/versao")
    public Curso buscaVersao(@RequestParam String versao) {
        return cursoService.buscarPorVersao(versao);
    }

    @GetMapping("/busca/vigente")
    public Curso buscaVigente() {
        return cursoService.buscarVigente();
    }

    @GetMapping("/historico")
    public List<Curso> listaHistorico() {
        return cursoService.listaHistorico();
    }
}
