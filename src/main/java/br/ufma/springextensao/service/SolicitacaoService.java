package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Solicitacao;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.SolicitacaoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SolicitacaoService {
    @Autowired
    SolicitacaoRepo solicitacaoRepo;

    @Autowired
    UsuarioService usuarioService;

    /**
     * Essa função cria uma nova solicitação
     * @param solicitacao objeto para transferir informação
     * @return Solicitação persistida no banco
     **/
    public Solicitacao submeter(SolicitacaoDTO solicitacao) {
        Solicitacao solicitacaoNovo;
        Discente discente = (Discente) usuarioService.buscarPorId(solicitacao.getIdDiscente());

        if (solicitacao.getDataSolicitacao() == null || solicitacao.getDescricao() == null || discente == null) {
            throw new IllegalArgumentException();
        }

        solicitacaoNovo = Solicitacao.builder().
                descricao(solicitacao.getDescricao()).
                discente(discente).
                dataSolicitacao(LocalDate.parse(solicitacao.getDataSolicitacao())).
                build();

        return solicitacaoRepo.save(solicitacaoNovo);
    }

    /**
     * Essa função aprova uma solicitação
     * @param
     * @return true se foi criada com sucesso, falso caso contrário
     **/
    public boolean aprovar() {
        // checagem de permissão
        return false;
    }

    /**
     * Essa função
     * @param
     * @return
     **/
    public boolean indeferir() {
        return false;
    }

    /**
     * Essa função
     * @param
     * @return
     **/
    public boolean reenviar() {
        return false;
    }

    /**
     * Essa função busca uma solicitação por seu id
     * @param id id da solicitação desejada
     * @return nulo se não for achada, a solicitação desejada
     **/
    public Solicitacao buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException();
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
            throw new IllegalArgumentException();
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
