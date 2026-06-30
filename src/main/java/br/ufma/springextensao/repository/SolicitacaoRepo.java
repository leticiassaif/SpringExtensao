package br.ufma.springextensao.repository;

import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitacaoRepo
        extends JpaRepository<Solicitacao, Integer> {
    List<Solicitacao> findByStatus(Status status);
    List<Solicitacao> findByDiscente(Discente discente);
    List<Solicitacao> findByDiscenteAndStatus(Discente discente, Status status);
}
