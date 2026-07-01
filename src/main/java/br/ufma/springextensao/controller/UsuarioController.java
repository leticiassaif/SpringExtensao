package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.PainelHorasDTO;
import jakarta.servlet.http.HttpSession;
import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.UsuarioDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Docente;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static br.ufma.springextensao.util.Sessao.logado;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {
    @Autowired
    UsuarioService usuarioService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Usuario login (@RequestBody UsuarioDTO loginDTO, HttpSession session) {
        Usuario usuario = usuarioService.autenticar(loginDTO.getEmail(), loginDTO.getSenha());
        session.setAttribute("IdUsuarioLogado", usuario.getId());
        return usuario;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout (HttpSession session) {
        session.invalidate();
    }

    @PostMapping("/cadastrar/discente")
    @ResponseStatus(HttpStatus.CREATED)
    public Discente cadastrarDiscente(@RequestBody DiscenteDTO discente) {
        return usuarioService.cadastrarDiscente(discente);
    }

    @PostMapping("/cadastrar/docente")
    @ResponseStatus(HttpStatus.CREATED)
    public Docente cadastrarDocente(@RequestBody DocenteDTO docente, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return usuarioService.cadastrarDocente(solicitante, docente);
    }

    @PatchMapping("/promover/docente/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Docente promoverDocente(@PathVariable Integer id, @RequestParam String cargo, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return usuarioService.promoverDocente(solicitante, cargo, id);
    }

    @PatchMapping("/desativar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Usuario desativar(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return usuarioService.desativar(solicitante, id);
    }

    @PatchMapping("/anonimizar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Usuario anonimizar(@PathVariable Integer id, HttpSession session) {
        Usuario solicitante = logado(session, usuarioService);
        return usuarioService.anonimizar(solicitante, id);
    }

    @GetMapping("/email/{email}")
    public Usuario buscaEmail(@PathVariable String email) {
        return usuarioService.buscarPorEmail(email);
    }

    @GetMapping("/id/{id}")
    public Usuario bucaId(@PathVariable Integer id) {
        return usuarioService.buscarPorId(id);
    }

    @GetMapping("/painel/{id}")
    public PainelHorasDTO painelHorasDTO(@PathVariable Integer id) {
        return usuarioService.painelHorasDTO(id);
    }
}
