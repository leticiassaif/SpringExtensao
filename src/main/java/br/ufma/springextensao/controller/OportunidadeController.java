package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.OportunidadeRepo;
import br.ufma.springextensao.service.OportunidadeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private OportunidadeService service;

    @PostMapping
    public Oportunidade criaOportunidade(@RequestBody OportunidadeDTO dto) {
        return service.criaOportunidade(dto);
    }

    @PostMapping("/publicar/{id}")
    public Oportunidade publicarOportunidade(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return service.publicarOportunidade(id, solicitante);
    }

    @PostMapping("/aprovar/{id}")
    public Oportunidade aprovarOportunidade(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return service.aprovarOportunidade(id, solicitante);
    }

    @GetMapping("/oportunidade")
    public List<Oportunidade> listarOportunidades() {
        return service.listarOportunidades();
    }

}
