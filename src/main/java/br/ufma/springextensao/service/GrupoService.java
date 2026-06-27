package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.GrupoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static br.ufma.springextensao.service.UsuarioService.hasPermissao;

@Service
public class GrupoService {
    @Autowired
    GrupoRepo grupoRepo;

    @Autowired
    PapelRepo papelRepo;

    @Autowired
    UsuarioRepo usuarioRepo;

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
        Usuario usuarioDoc = usuarioService.buscarPorId(grupo.getIdResponsavel());
        Usuario usuarioDir = usuarioService.buscarPorId(grupo.getIdDiretor());
        //Curso curso = cursoService.buscaPorId(grupo.getIdCurso());

        if (usuarioDoc == null || usuarioDir == null) {
            throw new IllegalArgumentException("Usuário(s) não existe.");
        }

        if (!(usuarioDoc instanceof Docente docente)) {
            throw new IllegalArgumentException("Usuário não é docente.");
        }

        if (!(usuarioDir instanceof Discente diretor)) {
            throw new IllegalArgumentException("Usuário não é discente.");
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
                diretores(new ArrayList<>()).
                discentesGrupo(new ArrayList<>()).
                status(Status.PENDENTE).
                build();

        // adiciona o diretor do grupo sem promover ainda - só para o grupo não ser aprovado sem um diretor
        grupoNovo.getDiretores().add(diretor);

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
        Papel diretor = papelRepo.findByNome("DIRETOR");

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
        Discente disDiretor = grupo.getDiretores().get(0);

        if (!disDiretor.getCargos().contains(diretor)) {
            usuarioService.promoverDiscente(disDiretor.getId());
        }

        grupo.getDiscentesGrupo().add(disDiretor);

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

    /**
     * Essa função adiciona um discente ao grupo
     * @param solicitante quem chamou a função
     * @param idGrupo id do grupo
     * @param idDiscente id do discente que se deseja adicionar
     * @return discente persistido no banco
     **/
    @Transactional
    public Grupo adicionarMembro(Usuario solicitante, Integer idGrupo, Integer idDiscente) {
        Grupo grupo = buscaPorId(idGrupo);
        Usuario usuario = usuarioService.buscarPorId(idDiscente);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (!permissaoGrupo(grupo, solicitante)) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        if (grupo.getStatus() != Status.APROVADO) {
            throw new IllegalArgumentException("Grupo precisa estar ativo/aprovado.");
        }

        if (membroPertenceGrupo(grupo, discente)) {
            throw new IllegalArgumentException("O discente já faz parte do grupo.");
        }

        if (!discente.isAtivo()) {
            throw new IllegalArgumentException("O novo membro precisa ser um usuário ativo.");
        }

        grupo.getDiscentesGrupo().add(discente);
        discente.getGrupos().add(grupo);

        usuarioRepo.save(discente);
        return grupoRepo.save(grupo);
    }

    /**
     * Essa função remove um discente de um grupo
     * @param solicitante quem chamou a função
     * @param idGrupo id do grupo
     * @param idDiscente id do discente que se deseja remover
     * @return discente persistido no banco
     **/
    @Transactional
    public Grupo removerMembro(Usuario solicitante, Integer idGrupo, Integer idDiscente) {
        Grupo grupo = buscaPorId(idGrupo);
        Usuario usuario = usuarioService.buscarPorId(idDiscente);

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (!permissaoGrupo(grupo, solicitante)) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        if (grupo.getStatus() != Status.APROVADO) {
            throw new IllegalArgumentException("Grupo precisa estar ativo/aprovado.");
        }

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        if (!membroPertenceGrupo(grupo, discente)) {
            throw new IllegalArgumentException("O discente não faz parte do grupo.");
        }

        grupo.getDiscentesGrupo().remove(discente);
        discente.getGrupos().remove(grupo);

        usuarioRepo.save(discente);
        return grupoRepo.save(grupo);
    }

    /**
     * Essa função atribui um cargo ao discente em um grupo
     * @param solicitante quem chamou a função
     * @param idGrupo id do grupo
     * @param idDiscente id do discente que se deseja remover
     * @param cargo cargo que se deseja atribuir
     * @return grupo persistido no banco
     **/
    @Transactional
    public Grupo atribuirCargo(Usuario solicitante, Integer idDiscente, Integer idGrupo, String cargo) {
        if (cargo == null || cargo.isBlank()) {
            throw new IllegalArgumentException("Cargo inválido.");
        }

        Papel papel = papelRepo.findByNome(cargo.toUpperCase());

        if (papel == null) {
            throw new IllegalArgumentException("Papel não existe.");
        }

        Grupo grupo = buscaPorId(idGrupo);

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (!solicitante.equals(grupo.getResponsavel())) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        if (grupo.getStatus() != Status.APROVADO) {
            throw new IllegalArgumentException("Grupo precisa estar ativo/aprovado.");
        }

        Usuario usuario = usuarioService.buscarPorId(idDiscente);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        if (!membroPertenceGrupo(grupo, discente)) {
            throw new IllegalArgumentException("Discente não faz parte do grupo.");
        }

        if (!discente.getCargos().contains(papel)) {
            discente.getCargos().add(papel);
            usuarioRepo.save(discente);
        }

        grupo.getDiretores().add(discente);
        return grupoRepo.save(grupo);
    }

    /**
     * Essa função remove um cargo de um discente
     * @param solicitante quem chamou a função
     * @param idGrupo id do grupo
     * @param idDiscente id do discente que se deseja remover
     * @param cargo cargo que se deseja atribuir
     * @return grupo persistido no banco
     **/
    @Transactional
    public Grupo removerCargo(Usuario solicitante, Integer idDiscente, Integer idGrupo, String cargo) {
        if (cargo == null || cargo.isBlank()) {
            throw new IllegalArgumentException("Cargo inválido.");
        }

        Papel papel = papelRepo.findByNome(cargo.toUpperCase());

        if (papel == null) {
            throw new IllegalArgumentException("Papel não existe.");
        }

        Grupo grupo = buscaPorId(idGrupo);

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (!solicitante.equals(grupo.getResponsavel())) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        if (grupo.getStatus() != Status.APROVADO) {
            throw new IllegalArgumentException("Grupo precisa estar ativo/aprovado.");
        }

        Usuario usuario = usuarioService.buscarPorId(idDiscente);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        if (!grupo.getDiretores().contains(discente)) {
            throw new IllegalArgumentException("Discente não faz parte da diretoria.");
        }

        grupo.getDiretores().remove(discente);

        boolean isDiscenteDiretorOutroGrupo = discente.getGruposDiretores().stream().
                anyMatch(g -> !g.equals(grupo));

        if (!isDiscenteDiretorOutroGrupo) {
            discente.getCargos().remove(papel);
            usuarioRepo.save(discente);
        }

        discente.getGruposDiretores().remove(grupo);
        return grupoRepo.save(grupo);
    }

    /**
     * Essa função remove um discente de um grupo
     * @param solicitante quem chamou a função
     * @param id id do discente
     * @return discente persistido no banco
     **/
    @Transactional
    public Discente removerDiscenteTodosGrupos(Usuario solicitante, Integer id) {
//        Papel admin = papelRepo.findByNome("ADMIN");
//
//        if (!hasPermissao(solicitante, admin)) {
//            throw new SecurityException("Usuário não possui permissão.");
//        }

        Usuario usuario = usuarioService.buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        discente.getGrupos().forEach(grupo -> grupo.getDiscentesGrupo().remove(discente));
        grupoRepo.saveAll(discente.getGrupos());

        discente.getGrupos().clear();
        return usuarioRepo.save(discente);
    }

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

    // ou deixa public e retorna o discente?? se nao, tirar checagem?
    /**
     * Essa função checa se um discente faz parte de um grupo
     * @param grupo grupo que se deseja checar
     * @param discente discente em questão
     * @return true se pertence, falso caso contrário
     **/
    private boolean membroPertenceGrupo(Grupo grupo, Discente discente) {
        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (discente == null) {
            throw new IllegalArgumentException("Discente não existe.");
        }

        return discente.getGrupos().contains(grupo);
    }

    //public Discente buscarMembroPorCargo() {}

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

    /**
     * Essa função checa se o solicitante tem permissão para manipular os membros do grupo
     * @param grupo grupo que se deseja manipular
     * @param solicitante quem chamou a função
     * @return true se possui permissão, falso caso contrário
     **/
    private boolean permissaoGrupo(Grupo grupo, Usuario solicitante) {
        Papel admin = papelRepo.findByNome("ADMIN");

        boolean isSolicitanteAdmin = hasPermissao(solicitante, admin);
        // verificar
        boolean isSolicitanteDiretor = grupo.getDiretores().contains(solicitante);
        boolean isSolicitanteResponsavel = grupo.getResponsavel().equals(solicitante);

        return isSolicitanteDiretor && isSolicitanteResponsavel && isSolicitanteAdmin;
    }
}
