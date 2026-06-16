package br.ufma.springextensao.service;

import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.jvnet.hk2.annotations.Service;
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
}
