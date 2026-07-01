package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.OportunidadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import br.ufma.springextensao.service.UsuarioService;

import java.util.List;
import static br.ufma.springextensao.util.Sessao.logado;

@RestController
@RequestMapping("/api/oportunidade")
public class OportunidadeController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    OportunidadeService oportunidadeService;

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public Oportunidade criaOportunidade(@RequestBody OportunidadeDTO dto, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return oportunidadeService.criaOportunidade(solicitante, dto);
    }

    @PatchMapping("/publicar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Oportunidade publicarOportunidade(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return oportunidadeService.publicarOportunidade(id, solicitante);
    }

    @PatchMapping("/aprovar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Oportunidade aprovarOportunidade(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return oportunidadeService.aprovarOportunidade(id, solicitante);
    }

    @PatchMapping("/iniciar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Oportunidade iniciarOportunidade(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return oportunidadeService.iniciarOportunidade(id, solicitante);
    }

    @PatchMapping("/encerrar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Oportunidade encerrarOportunidade(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return oportunidadeService.encerrarOportunidade(id, solicitante);
    }

    @PatchMapping("/cancelar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Oportunidade cancelarOportunidade(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return oportunidadeService.cancelarOportunidade(id, solicitante);
    }

    @GetMapping("/oportunidade")
    public List<Oportunidade> listarOportunidades() {
        return oportunidadeService.listarOportunidades();
    }

}
