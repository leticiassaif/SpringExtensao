package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.GrupoMembroRepo;
import br.ufma.springextensao.repository.GrupoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    GrupoMembroRepo grupoMembroRepo;

    @Autowired
    UsuarioService usuarioService;

    /**
     * Essa função cria um novo grupo
     * @param grupo objeto para transferir informação
     * @param solicitante quem chamou a função
     * @return grupo persistido no banco
     **/
    // automaticamente aprovar se for chamado por um docente
    @Transactional
    public Grupo criar(GrupoDTO grupo, Usuario solicitante) {
        Grupo grupoNovo;
        Usuario usuarioDoc = usuarioService.buscarPorId(grupo.getIdResponsavel());
        Usuario usuarioDir = usuarioService.buscarPorId(grupo.getIdDiretor());

        if (usuarioDoc == null || usuarioDir == null) {
            throw new IllegalArgumentException("Usuário(s) não existe.");
        }

        if (!(usuarioDoc instanceof Docente docente)) {
            throw new IllegalArgumentException("Usuário não é docente.");
        }

        if (!(usuarioDir instanceof Discente diretor)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        grupoNovo = Grupo.builder().
                nome(grupo.getNome()).
                descricao(grupo.getDescricao()).
                email(grupo.getEmail()).
                //curso(curso).
                responsavel(docente).
                membros(new ArrayList<>()).
                status(Status.PENDENTE).
                build();

        return grupoRepo.save(grupoNovo);
    }

    /**
     * Essa função aprova um grupo
     * @param solicitante quem chamou a função
     * @param idGrupo id do grupo que se deseja aprovar
     * @param idDiretor id do discente diretor do grupo
     * @return grupo persistido no banco
     **/
    @Transactional
    public Grupo aprovar(Usuario solicitante, Integer idGrupo, Integer idDiretor) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        Grupo grupo = buscaPorId(idGrupo);
        Usuario usuario = usuarioService.buscarPorId(idDiretor);

        if (grupo == null) {
            throw new IllegalArgumentException("Grupo não existe.");
        }

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        if (grupo.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Grupo não está pendente");
        }

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)
                && !(solicitante.equals(grupo.getResponsavel()))) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        grupo.setStatus(Status.APROVADO);

        adicionarMembro(solicitante, discente.getId(), grupo.getId());
        atribuirCargo(solicitante, discente.getId(), grupo.getId(), "DIRETOR");

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

        Papel membro = papelRepo.findByNome("MEMBRO");

        GrupoMembro grupoMembro = GrupoMembro.builder().
                discente(discente).
                grupo(grupo).
                papelExercido(membro).
                dataInicio(LocalDate.now()).
                build();

        grupo.getMembros().add(discente);
        grupo.getMembrosHistorico().add(grupoMembro);

        discente.getGrupos().add(grupo);
        discente.getCargoHistorico().add(grupoMembro);

        grupoMembroRepo.save(grupoMembro);
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

        Papel diretor = papelRepo.findByNome("DIRETOR");
        Papel vice = papelRepo.findByNome("VICE-DIRETOR");
        Papel tesoureiro = papelRepo.findByNome("TESOUREIRO");

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

        List<GrupoMembro> membros = grupoMembroRepo.findByGrupoAndDiscente(grupo, discente);

        List<Papel> cargos = List.of(diretor, vice, tesoureiro);
        cargos.forEach(cargo -> {
            boolean hasCargo = membros.stream().
                    anyMatch(membro -> membro.getPapelExercido().equals(cargo));
            if (hasCargo) {
                removerCargo(solicitante, discente.getId(), grupo.getId(), cargo.getNome());
            }
        });

        grupo.getMembros().remove(discente);
        discente.getGrupos().remove(grupo);

        membros.forEach(m -> m.setDataFim(LocalDate.now())); // p/ cargo membro
        grupoMembroRepo.saveAll(membros);
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

        if (cargo.equals("ADMIN") || cargo.equals("COORDENADOR") || cargo.equals("MEMBRO")) {
            throw new IllegalArgumentException("Esse cargo não pode ser atribuido.");
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

        GrupoMembro membroNovo = GrupoMembro.builder().
                grupo(grupo).
                discente(discente).
                papelExercido(papel).
                dataInicio(LocalDate.now()).
                build();

        if (!discente.getCargos().contains(papel)) {
            discente.getCargos().add(papel);
        }

        discente.getCargoHistorico().add(membroNovo);
        grupo.getMembrosHistorico().add(membroNovo);

        usuarioRepo.save(discente);
        grupoMembroRepo.save(membroNovo);
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

        GrupoMembro membro = grupoMembroRepo.findByGrupoAndDiscenteAndPapel(grupo, discente, papel).orElse(null);

        if (membro == null) {
            throw new IllegalArgumentException("Discente não possui esse cargo.");
        }

        // checa se o discente possui o mesmo cargo em outro grupo em que é ativo
        boolean hasCargoOutroGrupo = discente.getCargoHistorico().stream().
                anyMatch(gm -> !gm.getGrupo().equals(grupo)
                && gm.getDataFim() == null && gm.getPapelExercido().equals(papel));

        if (!hasCargoOutroGrupo) {
            discente.getCargos().remove(papel);
            usuarioRepo.save(discente);
        }

        membro.setDataFim(LocalDate.now());

        grupoMembroRepo.save(membro);
        return grupoRepo.save(grupo);
    }

    /**
     * Essa função remove um discente de um grupo
     * @param solicitante quem chamou a função
     * @param id id do discente
     **/
    @Transactional
    public void removerDiscenteTodosGrupos(Usuario solicitante, Integer id) {
        Usuario usuario = usuarioService.buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        List<GrupoMembro> membros = grupoMembroRepo.findByDiscente(discente);

        membros.forEach(membro -> membro.setDataFim(LocalDate.now()));
        discente.getGrupos().forEach(grupo -> grupo.getMembros().remove(discente));
        discente.getGrupos().clear();

        grupoMembroRepo.saveAll(membros);
        grupoRepo.saveAll(discente.getGrupos());
        usuarioRepo.save(discente);
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

        return grupo.getMembros();
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
        Papel diretor = papelRepo.findByNome("DIRETOR");

        boolean isSolicitanteAdmin = hasPermissao(solicitante, admin);
        boolean isSolicitanteResponsavel = grupo.getResponsavel().equals(solicitante);
        boolean isSolicitanteDiretoria = false;

        if (solicitante instanceof Discente discente) {
            isSolicitanteDiretoria = grupo.getMembrosHistorico().stream().
                    anyMatch(gm -> gm.getPapelExercido().equals(diretor) &&
                            gm.getDataFim() == null && gm.getDiscente().equals(discente));
        }

        return isSolicitanteDiretoria || isSolicitanteResponsavel || isSolicitanteAdmin;
    }
}
