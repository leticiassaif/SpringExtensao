package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.UsuarioDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * @param docente objeto para transferir informação
     * @return Docente persistido no banco
     **/
    public Docente cadastrarDocente(DocenteDTO docente) {
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
        if (usuario == null || papel == null) {
            throw new IllegalArgumentException("Campo(s) inválido(s).");
        }
        return usuario.getCargos().contains(papel);
    }
}