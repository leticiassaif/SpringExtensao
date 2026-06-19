package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Solicitacao;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.SolicitacaoRepo;
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

    /**
     * Essa função cria uma nova solicitação
     * @param solicitacao objeto para transferir informação
     * @return Solicitação persistida no banco
     **/
    public Solicitacao submeter(SolicitacaoDTO solicitacao) {
        Solicitacao solicitacaoNovo;
        Discente discente = (Discente) usuarioService.buscarPorId(solicitacao.getIdDiscente());

        if (discente == null) {
            throw new IllegalArgumentException("Discente não existe");
        }

        solicitacaoNovo = Solicitacao.builder().
                descricao(solicitacao.getDescricao()).
                discente(discente).
                cargaHorario(solicitacao.getCargaHoraria()).
                dataSolicitacao(LocalDate.parse(solicitacao.getDataSolicitacao())).
                build();

        return solicitacaoRepo.save(solicitacaoNovo);
    }

    /**
     * Essa função aprova uma solicitação
     * @param solicitante quem chamou a função
     * @param id id da solicitação que se deseja aprovar
     **/
    public void aprovar(Usuario solicitante, Integer id) {
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

        solicitacao.setStatus(Status.APROVADO);
        solicitacao.setDataAtual(LocalDate.now());
        Discente discente = solicitacao.getDiscente();
        discente.setCargaHoraria(discente.getCargaHoraria() + solicitacao.getCargaHorario());

        solicitacaoRepo.save(solicitacao);
    }

    /**
     * Essa função indefere uma solicitação
     * @param solicitante quem chamou a função
     * @param id id da solicitação que se deseja indeferir
     * @param parecer ...
     **/
    public void indeferir(Usuario solicitante, Integer id, String parecer) {
        if (parecer == null) {
            throw new IllegalArgumentException("Parecer inválido.");
        }

        // fazer has permissão!

        Solicitacao solicitacao = buscarPorId(id);

        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação não existe.");
        }

        if (solicitacao.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Solicitação não está pendente");
        }

        solicitacao.setStatus(Status.INDEFERIDO);
        solicitacao.setParecer(parecer);
        // verificar como seria o período de 5 dias
    }

    /**
     * Essa função reenvia uma solicitação anteriormente indeferida
     * @param id id da solicitação que deseja reenviar
     **/
    public void reenviar(Integer id) {
        Solicitacao solicitacao = buscarPorId(id);

        if (solicitacao == null) {
            throw new IllegalArgumentException("Solicitação não existe.");
        }

        if (solicitacao.getStatus() != Status.INDEFERIDO) {
            throw new IllegalStateException("Solicitação não foi indeferida.");
        }

        solicitacao.setStatus(Status.PENDENTE);
        //solicitacao.setParecer(null);
        // verificar como seria o período de 10 dias
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
        Discente discente = (Discente) usuarioService.buscarPorId(id);

        if (discente == null) {
            throw new IllegalArgumentException("Discente não existe.");
        }

        return solicitacaoRepo.findByDiscente(discente);
    }

    /**
     * Essa função lista as solicitações com status de pendentes
     * @return lista com as solicitações pendentes
     **/
    public List<Solicitacao> listarPendentes() {
        return solicitacaoRepo.findByStatus(Status.PENDENTE);
    }
}
