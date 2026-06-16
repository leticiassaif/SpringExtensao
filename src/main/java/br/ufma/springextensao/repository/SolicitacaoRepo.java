package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitacaoRepo
        extends JpaRepository<Solicitacao, Integer> {
}
