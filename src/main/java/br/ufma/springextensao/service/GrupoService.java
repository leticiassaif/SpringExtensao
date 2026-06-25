package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.GrupoRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GrupoService {
    @Autowired
    GrupoRepo grupoRepo;

    @Autowired
    CursoService cursoService;

    @Autowired
    UsuarioService usuarioService;

    @Transactional
    public Grupo criar(GrupoDTO grupo) {
        Grupo grupoNovo;
        Docente docente = (Docente) usuarioService.buscarPorId(grupo.getIdResponsavel());
        //Curso curso = cursoService.buscaPorId(grupo.getIdCurso());

        if (docente == null) {
            throw new IllegalArgumentException("Docente não existe.");
        }

//        if (curso == null) {
//            throw new IllegalArgumentException("Curso não existe.");
//        }

        grupoNovo = Grupo.builder().
                nome(grupo.getNome()).
                descricao(grupo.getDescricao()).
                email(grupo.getEmail()).
                //curso(curso).
                responsavel(docente).
                build();

        return grupoRepo.save(grupoNovo);
    }

    public Grupo aprovar() {}

    public Grupo negar() {}

    public Discente adicionarMembro() {}

    public Discente removerMembro() {}

    public Discente atribuirCargo() {}

    public Discente removerDiscenteTodosGrupos() {}

    /**
     * Essa função busca um grupo por seu id
     * @param id id do grupo desejado
     * @return o grupo, nulo se não for achado
     **/
    public Grupo buscaPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return grupoRepo.findById(id).orElse(null);
    }

    // precisa?
    public Discente buscarMembroPorGrupo() {}

    public Discente buscarMembroPorCargo() {}

    /**
     * Essa função lista todos os grupos
     * @return lista com todos os grupos
     **/
    public List<Grupo> listaGrupos() {
        return grupoRepo.findAll();
    }

    /**
     * Essa função lista todos os discentes de um grupo
     * @param id id do grupo desejado
     * @return lista com todos os discentes do grupo
     **/
    public List<Usuario> listaGrupoMembros(Integer id) {
        Grupo grupo = buscaPorId(id);

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        return grupo.getDiscentesGrupo();
    }

    /**
     * Essa função lista todos os discentes ativos de um grupo
     * @param id id do grupo desejado
     * @return lista com todos os discentes ativos do grupo
     **/
    public List<Usuario> listaGrupoMembrosAtivos(Integer id) {
        return listaGrupoMembros(id).stream().filter(Usuario::isAtivo).toList();
    }

    /**
     * Essa função lista todos os discentes não ativos de um grupo
     * @param id id do grupo desejado
     * @return lista com todos os discentes não ativos do grupo
     **/
    public List<Usuario> listaGrupoMembrosNaoAtivos(Integer id) {
        return listaGrupoMembros(id).stream().filter(u -> !u.isAtivo()).toList();
    }

    // INCOMPLETA
    private static boolean permissaoGrupo(Grupo grupo, Discente discente) {
        return true;
    }
}
