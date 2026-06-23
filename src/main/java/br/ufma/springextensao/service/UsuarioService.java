package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.UsuarioDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static br.ufma.springextensao.util.Validacao.isEmailValido;

@Service
public class UsuarioService {
    @Autowired
    UsuarioRepo usuarioRepo;

    @Autowired
    CursoRepo cursoRepo;

    @Autowired
    PapelRepo papelRepo;

    /**
     * Essa função cadastra um novo discente
     * @param discente objeto para transferir informação
     * @return Discente persistido no banco
     **/
    public Discente cadastrarDiscente(DiscenteDTO discente) {
        Discente discenteNovo;
        Curso curso = cursoRepo.findById(discente.getIdCurso()).orElse(null);

        if (curso == null) {
            throw new IllegalArgumentException("Curso com esse ID não existe.");
        }

        discenteNovo = Discente.builder().
                nome(discente.getNome()).
                email(discente.getEmail()).
                senha(discente.getSenha()).
                ativo(true).
                matricula(discente.getMatricula()).
                cargaHoraria(discente.getCargaHoraria()).
                curso(curso).
                build();

        return usuarioRepo.save(discenteNovo);
    }

    /**
     * Essa função cadastra um novo docente
     * @param solicitante quem chamou a função
     * @param docente objeto para transferir informação
     * @return Docente persistido no banco
     **/
    public Docente cadastrarDocente(Usuario solicitante, DocenteDTO docente) {
        Papel admin = papelRepo.findByNome("ADMIN");

        if (!hasPermissao(solicitante, admin)) {
            throw new SecurityException("O solicitante não possui permissão para anonimizar o usuário");
        }

        Docente docenteNovo = Docente.builder().
                nome(docente.getNome()).
                email(docente.getEmail()).
                senha(docente.getSenha()).
                ativo(true).
                siape(docente.getSiape()).
                departamento(docente.getDepartamento()).
                build();

        return usuarioRepo.save(docenteNovo);
    }

    /**
     * Essa função promove um docente para um coordenador ou ...
     * @param id id do docente que deseja promover
     * @return docente persistido no banco
     **/
    @Transactional
    public Docente promoverDocente(String cargo, Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }

        if (cargo == null) {
            throw new IllegalArgumentException("Cargo inválido.");
        }

        cargo = cargo.toUpperCase();
        Papel papel = papelRepo.findByNome(cargo);

        if (papel == null) {
            throw new IllegalArgumentException("Cargo não existe.");
        }

        Docente docente = (Docente) buscarPorId(id);

        if (docente == null) {
            throw new IllegalArgumentException("Docente não existe.");
        }

        docente.getCargos().add(papel);

        return usuarioRepo.save(docente);
    }

    /**
     * Essa função promove um discente para discente diretor
     * @param id id do discente que deseja promover
     * @return discente persistido no banco
     **/
    @Transactional
    public Discente promoverDiscente(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }

        Discente discente = (Discente) buscarPorId(id);

        if (discente == null) {
            throw new IllegalArgumentException("Discente não existe.");
        }

        Papel diretor = papelRepo.findByNome("DISCENTE DIRETOR");
        discente.getCargos().add(diretor);

        return usuarioRepo.save(discente);
    }

    /**
     * Essa função desativa a conta de um usuário
     * @param solicitante quem chamou a função
     * @param id id do usuário
     **/
    @Transactional
    public void desativar(Usuario solicitante, Integer id) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        Usuario usuario = buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe");
        }

        if (!(hasPermissao(solicitante, coordenador) && usuario instanceof Discente)
                && !hasPermissao(solicitante, admin)) {
            throw new SecurityException("O solicitante não possui permissão para desativar o usuário");
        }

        usuario.setAtivo(false);

        if (usuario instanceof Discente) {
            // remover de grupos
        }

        usuarioRepo.save(usuario);
    }

    /**
     * Essa função anonimiza a conta de um usuário
     * @param solicitante quem chamou a função
     * @param id id do usuário
     **/
    @Transactional
    public void anonimizar(Usuario solicitante, Integer id) {
        Papel admin = papelRepo.findByNome("ADMIN");

        if (!hasPermissao(solicitante, admin)) {
            throw new SecurityException("O solicitante não possui permissão para anonimizar o usuário");
        }

        Usuario usuario = buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe");
        }

        usuario.setAtivo(false);
        usuario.setNome("Usuário Anonimizado");
        usuario.setEmail("anonimo_" + usuario.getId() + "@sistema.local");
        usuario.setSenha("");
        usuario.getCargos().clear();

        if (usuario instanceof Discente) {
            // remover de grupos
        }

        usuarioRepo.save(usuario);
    }

    public Usuario buscarPorEmail(String email) {
        if (!isEmailValido(email)) {
            throw new IllegalArgumentException("Email inválido.");
        }
        return usuarioRepo.findByEmail(email);
    }

    /**
     * Essa função busca um usuário por id
     * @param id o id do usuário que deseja achar
     * @return o usuário buscado, nulo se não existir
     **/
    public Usuario buscarPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return usuarioRepo.findById(id).orElse(null);
    }

    // public void imprimirProgresso(Discente discente)

    /**
     * Essa função retorna se um usuário possui um certo cargo
     * @param usuario usuário em questão
     * @param papel papel procurado
     * @return true se o usuário possuir, false caso contrário
     **/
    public static boolean hasPermissao(Usuario usuario, Papel papel) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário inválido.");
        }
        return usuario.getCargos().contains(papel);
    }
}