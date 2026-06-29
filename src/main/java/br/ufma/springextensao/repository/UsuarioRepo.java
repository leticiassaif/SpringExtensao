package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepo
        extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
}
