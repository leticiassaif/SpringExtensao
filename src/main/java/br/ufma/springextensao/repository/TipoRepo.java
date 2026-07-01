package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoRepo
        extends JpaRepository<Tipo, Integer> {
    Optional<Tipo> findByNome(String nome);
}
