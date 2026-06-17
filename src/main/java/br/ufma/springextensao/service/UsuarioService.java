package br.ufma.springextensao.service;

import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Docente;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;

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

    public Usuario buscarPorId() {}

    // public static boolean podeGerenciarUsuario
    // public void imprimirProgresso(Discente discente)

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
