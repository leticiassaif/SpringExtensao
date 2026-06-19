package br.ufma.springextensao.repository;

import br.ufma.springextensao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepo
        extends JpaRepository<Usuario, Integer> {
    Usuario findByEmail(String email);
}
