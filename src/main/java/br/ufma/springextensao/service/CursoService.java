package br.ufma.springextensao.service;

import br.ufma.springextensao.model.Curso;
import br.ufma.springextensao.repository.CursoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CursoService {
    @Autowired
    CursoRepo cursoRepo;

    public Curso buscaPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return cursoRepo.findById(id).orElse(null);
    }
}
