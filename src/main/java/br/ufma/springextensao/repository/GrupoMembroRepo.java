package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Grupo;
import br.ufma.springextensao.model.GrupoMembro;
import br.ufma.springextensao.model.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoMembroRepo
        extends JpaRepository<GrupoMembro, Integer> {
    List<GrupoMembro> findByDiscente(Discente discente);
    List<GrupoMembro> findByGrupoAndDiscente(Grupo grupo, Discente discente);
    Optional<GrupoMembro> findByGrupoAndDiscenteAndPapelExercido(Grupo grupo, Discente discente, Papel papel);
}
