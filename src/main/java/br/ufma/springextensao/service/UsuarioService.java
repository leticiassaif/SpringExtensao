package br.ufma.springextensao.service;

import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Docente;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    @Autowired
    UsuarioRepo usuarioRepo;

    public Discente cadastrarDiscente() {}

    public Docente cadastrarDocente() {}

    /**
     * Essa função promove um docente para um coordenador ou ...
     * @param
     * @return
     **/
    public void promoverDocente() {}

    public void promoverDiscente() {}

    public void desativar() {}

    public void anonimizar() {}

    public Usuario buscarPorEmail() {}

    /**
     * Essa função busca um usuário por id
     * @param id o id do usuário que deseja achar
     * @return o usuário buscado, nulo se não existir
     **/
    public Usuario buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return usuarioRepo.findById(id).orElse(null);
    }

    // public static boolean podeGerenciarUsuario
    // public void imprimirProgresso(Discente discente)

    public static boolean hasPermissao(Usuario usuario, Papel papel) {
        for (Papel p : usuario.getCargos()) {
            if (p == papel) {
                return true;
            }
        }
        return false;
    }
}
