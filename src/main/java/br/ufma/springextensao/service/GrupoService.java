package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.GrupoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static br.ufma.springextensao.service.UsuarioService.hasPermissao;

@Service
public class GrupoService {
    @Autowired
    GrupoRepo grupoRepo;

    @Autowired
    PapelRepo papelRepo;

    @Autowired
    CursoService cursoService;

    @Autowired
    UsuarioService usuarioService;

    /**
     * Essa função cria um novo grupo
     * @param grupo objeto para transferir informação
     * @return grupo persistido no banco
     **/
    // automaticamente aprovar se for chamado por um docente
    @Transactional
    public Grupo criar(GrupoDTO grupo) {
        Grupo grupoNovo;
        Docente docente = (Docente) usuarioService.buscarPorId(grupo.getIdResponsavel());
        Discente diretor = (Discente) usuarioService.buscarPorId(grupo.getIdDiretor());
        //Curso curso = cursoService.buscaPorId(grupo.getIdCurso());

        if (docente == null) {
            throw new IllegalArgumentException("Docente não existe.");
        }

        if (diretor == null) {
            throw new IllegalArgumentException("Discente não existe.");
        }

//        if (curso == null) {
//            throw new IllegalArgumentException("Curso não existe.");
//        }

        grupoNovo = Grupo.builder().
                nome(grupo.getNome()).
                descricao(grupo.getDescricao()).
                email(grupo.getEmail()).
                diretor(diretor).
                //curso(curso).
                responsavel(docente).
                status(Status.PENDENTE).
                build();

        return grupoRepo.save(grupoNovo);
    }

    /**
     * Essa função aprova um grupo
     * @param solicitante quem chamou a função
     * @param id id do grupo que se deseja aprovar
     * @return grupo persistido no banco
     **/
    @Transactional
    public Grupo aprovar(Usuario solicitante, Integer id) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");
        Papel diretor = papelRepo.findByNome("DISCENTE DIRETOR");

        Grupo grupo = buscaPorId(id);

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (grupo.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Grupo não está pendente");
        }

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)
                && !(solicitante.equals(grupo.getResponsavel()))) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        grupo.setStatus(Status.APROVADO);

        if (grupo.getDiretor().getCargos().contains(diretor)) {
            usuarioService.promoverDiscente(grupo.getDiretor().getId());
        }

        grupo.getDiscentesGrupo().add(grupo.getDiretor());

        return grupoRepo.save(grupo);
    }

    /**
     * Essa função rejeita um grupo
     * @param solicitante quem chamou a função
     * @param id id do grupo que se deseja rejeitar
     * @return grupo persistido no banco
     **/
    public Grupo rejeitar(Usuario solicitante, Integer id, String justificativa) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Justificativa é obrigatória.");
        }

        Grupo grupo = buscaPorId(id);

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (grupo.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Grupo não está pendente");
        }

        grupo.setStatus(Status.REJEITADO);
        grupo.setJustificativaNegacao(justificativa);

        return grupoRepo.save(grupo);
    }

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
