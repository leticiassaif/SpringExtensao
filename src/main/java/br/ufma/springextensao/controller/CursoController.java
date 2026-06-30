package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.CursoDTO;
import br.ufma.springextensao.controller.dtos.UCEDTO;
import br.ufma.springextensao.model.Curso;
import br.ufma.springextensao.model.UCE;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.CursoService;
import br.ufma.springextensao.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/curso")
public class CursoController {
    @Autowired
    private CursoService cursoService;
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/cadastrar")
    @ResponseStatus(HttpStatus.CREATED)
    public Curso cadastrarCurso(@RequestBody CursoDTO curso, HttpSession session) {
        Usuario solicitante = usuarioService.buscarPorId((Integer) session.getAttribute("IdUsuarioLogado"));
        if (solicitante == null) {
            throw new SecurityException("Usuário não está logado.");
        }
        return cursoService.cadastrarCurso(solicitante, curso);
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

    @PostMapping("/uce/cadastrar")
    @ResponseStatus(HttpStatus.CREATED)
    public UCE cadastrarUCE(@RequestBody UCEDTO uce, HttpSession session) {
        Usuario solicitante = usuarioService.buscarPorId((Integer) session.getAttribute("IdUsuarioLogado"));
        if (solicitante == null) {
            throw new SecurityException("Usuário não está logado.");
        }
        return cursoService.cadastrarUCE(solicitante, uce);
    }

    @GetMapping("/uce/busca/{id}")
    public List<UCE> buscaUCEporCurso(@PathVariable Integer id) {
        return cursoService.buscaUCEPorPPC(id);
    }
}
