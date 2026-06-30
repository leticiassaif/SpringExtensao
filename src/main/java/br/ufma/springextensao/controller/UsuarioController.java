package br.ufma.springextensao.controller;

import jakarta.servlet.http.HttpSession;
import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.UsuarioDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Docente;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.UsuarioService;
import jakarta.validation.constraints.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
        Usuario solicitante = usuarioService.buscarPorId((Integer) session.getAttribute("IdUsuarioLogado"));
        if (solicitante == null) {
            throw new SecurityException("Usuário não está logado.");
        }
        return usuarioService.cadastrarDocente(solicitante, docente);
    }

    @PatchMapping("/promover/docente/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Docente promoverDocente(@PathVariable Integer id, @RequestParam String cargo) {
        return usuarioService.promoverDocente(, cargo, id);
    }

    @PatchMapping("/desativar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Usuario desativar(@PathVariable Integer id) {
        return usuarioService.desativar(, id);
    }

    @PatchMapping("/anonimizar/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Usuario anonimizar(@PathVariable Integer id) {
        return usuarioService.anonimizar(, id);
    }

    @GetMapping("/email/{email}")
    public Usuario buscaEmail(@PathVariable String email) {
        return usuarioService.buscarPorEmail(email);
    }

    @GetMapping("/id/{id}")
    public Usuario bucaId(@PathVariable Integer id) {
        return usuarioService.buscarPorId(id);
    }
}
