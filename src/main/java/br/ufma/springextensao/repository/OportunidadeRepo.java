package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Oportunidade;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OportunidadeRepo
        extends JpaRepository<Oportunidade, Integer> {
}
