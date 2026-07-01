package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Solicitacao;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.SolicitacaoRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static br.ufma.springextensao.service.UsuarioService.hasPermissao;

@Service
public class SolicitacaoService {
    @Autowired
    SolicitacaoRepo solicitacaoRepo;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    PapelRepo papelRepo;
    @Autowired
    private UsuarioRepo usuarioRepo;

    /**
     * Essa função cria uma nova solicitação
     * @param solicitacao objeto para transferir informação
     * @return Solicitação persistida no banco
     **/
    @Transactional
    public Solicitacao submeter(SolicitacaoDTO solicitacao) {
        Solicitacao solicitacaoNovo;
        Usuario usuario = usuarioService.buscarPorId(solicitacao.getIdDiscente());

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        if (solicitacao.getDescricao() == null || solicitacao.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição da solicitação inválida.");
        }

        if (solicitacao.getDataSolicitacao() == null || solicitacao.getDataSolicitacao().isBlank()) {
            throw new IllegalArgumentException("Data de solicitação inválida.");
        }

        if (solicitacao.getCargaHoraria() == null || solicitacao.getCargaHoraria() <= 0) {
            throw new IllegalArgumentException("Carga horária deve ser positiva.");
        }

        LocalDate dataSolicitacao;
        try {
            dataSolicitacao = LocalDate.parse(solicitacao.getDataSolicitacao());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Data de solicitação inválida.");
        }

        solicitacaoNovo = Solicitacao.builder().
                descricao(solicitacao.getDescricao()).
                discente(discente).
                cargaHorario(solicitacao.getCargaHoraria()).
                dataSolicitacao(dataSolicitacao).
                dataAtual(LocalDate.now()).
                status(Status.PENDENTE).
                build();

        return solicitacaoRepo.save(solicitacaoNovo);
    }

    /**
     * Essa função aprova uma solicitação
     * @param solicitante quem chamou a função
     * @param id id da solicitação que se deseja aprovar
     * @return solicitação persistida no banco
     **/
    @Transactional
    public Solicitacao aprovar(Usuario solicitante, Integer id) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        Solicitacao solicitacao = buscarPorId(id);

        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação não existe.");
        }

        if (solicitacao.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Solicitação não está pendente");
        }

        if (solicitacao.getCargaHorario() == null || solicitacao.getCargaHorario() < 0) {
            throw new IllegalArgumentException("Carga horária da solicitação inválida.");
        }

        solicitacao.setStatus(Status.APROVADO);
        solicitacao.setDataAtual(LocalDate.now());

        Discente discente = solicitacao.getDiscente();
        Integer cargaAtual = discente.getCargaHoraria() != null ? discente.getCargaHoraria() : 0;
        discente.setCargaHoraria(cargaAtual + solicitacao.getCargaHorario());

        return solicitacaoRepo.save(solicitacao);
    }

    /**
     * Essa função indefere uma solicitação
     * @param solicitante quem chamou a função
     * @param id id da solicitação que se deseja indeferir
     * @param parecer parecer da solicitação indeferida
     * @return solicitação persistida no banco
     **/
    @Transactional
    public Solicitacao indeferir(Usuario solicitante, Integer id, String parecer) {
        if (parecer == null || parecer.isBlank()) {
            throw new IllegalArgumentException("Parecer inválido.");
        }

        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)) {
            throw new SecurityException("Usuário não possui permissão.");
        }

        Solicitacao solicitacao = buscarPorId(id);

        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação não existe.");
        }

        if (solicitacao.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Solicitação não está pendente");
        }

        LocalDate hoje = LocalDate.now();

        solicitacao.setStatus(Status.INDEFERIDO);
        solicitacao.setDataAtual(hoje);
        solicitacao.setParecer(parecer);
        solicitacao.setPrazoReenvio(hoje.plusDays(5));

        return solicitacaoRepo.save(solicitacao);
    }

    /**
     * Essa função reenvia uma solicitação anteriormente indeferida
     * @param id id da solicitação que deseja reenviar
     * @return solicitação persistida no banco
     **/
    @Transactional
    public Solicitacao reenviar(Integer id) {
        Solicitacao solicitacao = buscarPorId(id);

        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação não existe.");
        }

        if (solicitacao.getStatus() != Status.INDEFERIDO) {
            throw new IllegalStateException("Solicitação não foi indeferida.");
        }

        if (solicitacao.getPrazoReenvio() == null) {
            throw new IllegalStateException("Solicitação não possui prazo de reenvio definido.");
        }

        LocalDate hoje = LocalDate.now();

        if (solicitacao.getPrazoReenvio().isBefore(hoje)) {
            solicitacao.setStatus(Status.CANCELADO);
            solicitacao.setParecer("O Aluno não fez o reenvio a tempo");
        } else {
            solicitacao.setStatus(Status.PENDENTE);
            solicitacao.setParecer(null);
        }

        solicitacao.setPrazoReenvio(null);
        solicitacao.setDataAtual(hoje);

        // verificar como seria o período de 10 dias

        return solicitacaoRepo.save(solicitacao);
    }

    /**
     * Essa função busca uma solicitação por seu id
     * @param id id da solicitação desejada
     * @return nulo se não for achada, a solicitação desejada
     **/
    public Solicitacao buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return solicitacaoRepo.findById(id).orElse(null);
    }

    /**
     * Essa função lista as solicitações feitas por um discente específico
     * @param id id do discente procurado
     * @return solicitações feitas pelo discente
     **/
    public List<Solicitacao> listarPorDiscente(Integer id) {
        Usuario usuario = usuarioService.buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }

        return solicitacaoRepo.findByDiscente(discente);
    }

    /**
     * Essa função retorna as Solicitações indeferidas do discente
     * @param id id do discente
     * @return lista com as solicitações indeferidas
     */
    public List<Solicitacao> listarIndeferidos(Integer id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe");
        }
        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }
        return solicitacaoRepo.findByDiscenteAndStatus(discente, Status.INDEFERIDO);
    }

    /**
     * Essa função lista as solicitações com status de pendentes
     * @return lista com as solicitações pendentes
     **/
    public List<Solicitacao> listarPendentes() {
        return solicitacaoRepo.findByStatus(Status.PENDENTE);
    }
}
