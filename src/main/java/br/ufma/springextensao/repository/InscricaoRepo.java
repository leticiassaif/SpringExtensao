package br.ufma.springextensao.repository;

import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Inscricao;
import br.ufma.springextensao.model.Oportunidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscricaoRepo
        extends JpaRepository<Inscricao, Integer> {
    Optional<Inscricao> findByDiscenteAndOportunidade(Discente discente, Oportunidade oportunidade);

    List<Inscricao> findByOportunidade(Oportunidade oportunidade);

    List<Inscricao> findByDiscente(Discente discente);

    List<Inscricao> findByOportunidadeAndStatus(Oportunidade oportunidade, Enum<Status> status);

}
