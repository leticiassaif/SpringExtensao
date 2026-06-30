package br.ufma.springextensao.util;

import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.UsuarioService;
import jakarta.servlet.http.HttpSession;

public class Sessao {
    public static Usuario logado(HttpSession session, UsuarioService service) {
        if (!(session.getAttribute("IdUsuarioLogado") instanceof Integer id)) {
            throw new SecurityException("É preciso estar logado para chamar esse método.");
        }

        Usuario solicitante = service.buscarPorId(id);

        if (solicitante == null) {
            throw new IllegalArgumentException("Solicitante não foi encontrado.");
        }

        return solicitante;
    }
}
