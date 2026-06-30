package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Inscricao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InscricaoRepo
        extends JpaRepository<Inscricao, Integer> {
}
