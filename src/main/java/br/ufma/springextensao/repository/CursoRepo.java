package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CursoRepo
        extends JpaRepository<Curso, Integer> {
}
