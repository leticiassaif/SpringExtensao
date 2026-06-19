package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.UsuarioDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static br.ufma.springextensao.util.Validacao.isEmailValido;

@Service
public class UsuarioService {
    @Autowired
    UsuarioRepo usuarioRepo;

    @Autowired
    CursoRepo cursoRepo;

    /**
     * Essa função cadastra um novo discente
     * @param discente objeto para transferir informação
     * @return Discente persistido no banco
     **/
    public Discente cadastrarDiscente(DiscenteDTO discente) {
        Discente discenteNovo;
        Curso curso = cursoRepo.findById(discente.getIdCurso()).orElse(null);

        if (curso == null) {
            throw new IllegalArgumentException("Curso com esse ID não existe.");
        }

        discenteNovo = Discente.builder().
                nome(discente.getNome()).
                email(discente.getEmail()).
                senha(discente.getSenha()).
                ativo(true).
                matricula(discente.getMatricula()).
                cargaHoraria(discente.getCargaHoraria()).
                curso(curso).
                build();

        return usuarioRepo.save(discenteNovo);
    }

    /**
     * Essa função cadastra um novo docente
     * @param solicitante quem chamou a função
     * @param docente objeto para transferir informação
     * @return Docente persistido no banco
     **/
    public Docente cadastrarDocente(Usuario solicitante, DocenteDTO docente) {
        // fazer has permissao
        Docente docenteNovo = Docente.builder().
                nome(docente.getNome()).
                email(docente.getEmail()).
                senha(docente.getSenha()).
                ativo(true).
                siape(docente.getSiape()).
                departamento(docente.getDepartamento()).
                build();

        return usuarioRepo.save(docenteNovo);
    }

    /**
     * Essa função promove um docente para um coordenador ou ...
     * @param
     * @return
     **/
    public void promoverDocente() {}

    /**
     * Essa função promove um discente para discente diretor
     * @param
     * @return
     **/
    public void promoverDiscente() {}

    /**
     * Essa função desativa a conta de um usuário
     * @param solicitante quem chamou a função
     * @param id id do usuário
     **/
    public void desativar(Usuario solicitante, Integer id) {
        // fazer has permissao

        Usuario usuario = buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe");
        }

        usuario.setAtivo(false);

        // checar se conta é discente ou docente!
    }

    /**
     * Essa função anonimiza a conta de um usuário
     * @param solicitante quem chamou a função
     * @param id id do usuário
     **/
    public void anonimizar(Usuario solicitante, Integer id) {
        // fazer has permissao

        Usuario usuario = buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe");
        }

        usuario.setAtivo(false);
        usuario.setNome("Usuário Anonimizado");
        usuario.setEmail("anonimo_" + usuario.getId() + "@sistema.local");
        usuario.setSenha("");
        usuario.getCargos().clear();

        // checar se conta é discente ou docente!
    }

    public Usuario buscarPorEmail(String email) {
        if (!isEmailValido(email)) {
            throw new IllegalArgumentException("Email inválido.");
        }
        return usuarioRepo.findByEmail(email);
    }

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

    // public void imprimirProgresso(Discente discente)

    public static boolean hasPermissao(Usuario usuario, Papel papel) {
        if (usuario == null || papel == null) {
            throw new IllegalArgumentException("Campo(s) inválido(s).");
        }
        return usuario.getCargos().contains(papel);
    }
}