package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Curso;
import br.ufma.springextensao.model.UCE;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UCERepo
        extends JpaRepository<UCE,Integer> {
    List<UCE> findByCurso(Curso curso);
}
