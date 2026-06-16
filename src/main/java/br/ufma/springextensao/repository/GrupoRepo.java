package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrupoRepo
        extends JpaRepository<Grupo, Integer> {
}
