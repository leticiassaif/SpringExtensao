package br.ufma.springextensao.service;

import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class UsuarioService {
    @Autowired
    UsuarioRepo usuarioRepo;

    // EXEMPLO USADO POR GERALDO:
    /**
     * Essa função busca um usuário por id
     * @param id chave do usuário única
     * @return nulo se não existir, Usuário na base se existir
     */
    public Usuario obterUsuarioPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        return usuarioRepo.findById(id).orElse(null);
    }

    /**
     * Essa função olha se o usuario informado possui permissão
     * @param usuario usuario que está sendo perguntado,
     * @param papel o cargo procurado,
     * @return true caso tenha e false caso não tenha
     */

    public static boolean hasPermissao(Usuario usuario, Papel papel) {
        return usuario.getCargos().contains(papel);
    }
}
