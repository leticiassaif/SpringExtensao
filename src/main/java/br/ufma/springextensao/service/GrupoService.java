package br.ufma.springextensao.service;

import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Grupo;
import br.ufma.springextensao.repository.GrupoRepo;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class GrupoService {
    @Autowired
    GrupoRepo grupoRepo;

    public Grupo criar() {}

    public Grupo aprovar() {}

    public Grupo negar() {}

    public Discente adicionarMembro() {}

    public Discente removerMembro() {}

    public Discente atribuirCargo() {}

    public Discente removerDiscenteTodosGrupos() {}

    /**
     * Essa função busca um grupo por seu id
     * @param id id do grupo desejado
     * @return o grupo, nulo se não for achado
     **/
    public Grupo buscaPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return grupoRepo.findById(id).orElse(null);
    }

    public Discente buscarMembroPorGrupo() {}

    public Discente buscarMembroPorCargo() {}

    /**
     * Essa função lista todos os grupos
     * @return lista com todos os grupos
     **/
    public List<Grupo> listaGrupos() {
        return grupoRepo.findAll();
    }

    // lista mebros

    // listar membros atviso de um grupo

    // listar membros nao atviso de um grupo

    private static boolean permissaoGrupo(Grupo grupo, Discente discente) {
        return true;
    }
}
