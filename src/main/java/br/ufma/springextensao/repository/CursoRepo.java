package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Curso;
import br.ufma.springextensao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepo
        extends JpaRepository<Curso, Integer> {
    @Query("SELECT c from Curso c WHERE c.dataFim IS NULL")
    Optional<Usuario> findVigente();

    @Query("SELECT c from Curso c ORDER BY c.dataInicio DESC")
    List<Curso> findHistorico();

    Optional<Curso> findByVersao(String curriculo);
}
