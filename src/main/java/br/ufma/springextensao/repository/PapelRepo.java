package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapelRepo
        extends JpaRepository<Papel, Integer> {
    Papel findByNome(String nome);
}
