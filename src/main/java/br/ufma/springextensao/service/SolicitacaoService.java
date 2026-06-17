package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Solicitacao;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.SolicitacaoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        solicitacaoNovo = Solicitacao.builder().
                descricao(solicitacao.getDescricao()).
                discente(discente).
                // data da solicitação?
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

    public boolean indeferir() {
        return false;
    }

    public boolean reenviar() {
        return false;
    }

    public Solicitacao buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        return solicitacaoRepo.findById(id).orElse(null);
    }

    // fazer no repo métodos para achar por status da solicitação (listar pendentes)
}
