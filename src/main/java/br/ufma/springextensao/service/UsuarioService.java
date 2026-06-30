package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.PainelHorasDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Usuario;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

import static br.ufma.springextensao.util.Validacao.isEmailValido;

@Service
public class UsuarioService {
    @Autowired
    UsuarioRepo usuarioRepo;

    @Autowired
    CursoRepo cursoRepo;

    @Autowired
    PapelRepo papelRepo;

    @Autowired
    GrupoService grupoService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Essa função cadastra um novo discente
     * @param discente objeto para transferir informação
     * @return Discente persistido no banco
     **/
    public Discente cadastrarDiscente(DiscenteDTO discente) {
        Discente dis;
        Curso curso = cursoRepo.findById(discente.getIdCurso()).orElse(null);

        if (curso == null) {
            throw new IllegalArgumentException("Curso com esse ID não existe.");
        }

        String hash = encoder.encode(discente.getSenha());

        dis = Discente.builder().
                nome(discente.getNome()).
                email(discente.getEmail()).
                senha(hash).
                ativo(true).
                cargos(new ArrayList<>()).
                matricula(discente.getMatricula()).
                cargaHoraria(discente.getCargaHoraria()).
                curso(curso).
                solicitacoes(new ArrayList<>()).
                grupos(new ArrayList<>()).
                cargoHistorico(new ArrayList<>()).
                oportunidades(new ArrayList<>()).
                build();

        return usuarioRepo.save(dis);
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

        String hash = encoder.encode(docente.getSenha());

        Docente docenteNovo = Docente.builder().
                nome(docente.getNome()).
                email(docente.getEmail()).
                senha(hash).
                ativo(true).
                cargos(new ArrayList<>()).
                siape(docente.getSiape()).
                departamento(docente.getDepartamento()).
                oportunidades(new ArrayList<>()).
                grupos(new ArrayList<>()).
                build();

        return usuarioRepo.save(docenteNovo);
    }

    /**
     * Essa função promove um docente para um coordenador ou ...
     * @param id id do docente que deseja promover
     * @return docente persistido no banco
     **/
    @Transactional
    public Docente promoverDocente(Usuario solicitante, String cargo, Integer id) {
        Papel admin = papelRepo.findByNome("ADMIN");

        if (!hasPermissao(solicitante, admin)) {
            throw new SecurityException("O usuário não possui permissão para promover um docente.");
        }

        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }

        if (cargo == null || cargo.isBlank()) {
            throw new IllegalArgumentException("Cargo inválido.");
        }

        cargo = cargo.toUpperCase();
        Papel papel = papelRepo.findByNome(cargo);

        if (papel == null) {
            throw new IllegalArgumentException("Cargo não existe.");
        }

        Usuario usuario = buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Docente docente)) {
            throw new IllegalArgumentException("Usuário não é docente.");
        }

        if (!docente.getCargos().contains(papel)) {
            docente.getCargos().add(papel);
        }

        return usuarioRepo.save(docente);
    }

//    /**
//     * Essa função promove um discente para discente diretor
//     * @param id id do discente que deseja promover
//     * @return discente persistido no banco
//     **/
//    @Transactional
//    public Discente promoverDiscente(Integer id) {
//        // verificar se precisa de hasPermissao
//        if (id == null) {
//            throw new IllegalArgumentException("ID inválido.");
//        }
//
//        Usuario usuario = buscarPorId(id);
//
//        if (usuario == null) {
//            throw new IllegalArgumentException("Usuário não existe.");
//        }
//
//        if (!(usuario instanceof Discente discente)) {
//            throw new IllegalArgumentException("Usuário não é discente.");
//        }
//
//        Papel diretor = papelRepo.findByNome("DIRETOR");
//        discente.getCargos().add(diretor);
//
//        return usuarioRepo.save(discente);
//    }

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
            grupoService.removerDiscenteTodosGrupos(solicitante, usuario.getId());
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
            grupoService.removerDiscenteTodosGrupos(solicitante, usuario.getId());
        }

        usuarioRepo.save(usuario);
    }

    /**
     * Essa função autentica um usuário no login
     * @param email o email do usuário que se deseja achar
     * @param senha a senha do usuário em formato hash
     * @return o usuário buscado, nulo se não existir
     **/
    public Usuario autenticar(String email, String senha) {
        if (!isEmailValido(email)) {
            throw new IllegalArgumentException("Email inválido.");
        }

        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha inválida.");
        }

        Usuario usuario = buscarPorEmail(email);

        if (usuario == null) {
            throw new IllegalArgumentException("Nenhum usuário possui esse email.");
        }

        if (!encoder.matches(senha, usuario.getSenha())) {
            throw new SecurityException("Senha incorreta.");
        }

        return usuario;
    }

    /**
     * Essa função busca um usuário por email
     * @param email o email do usuário que deseja achar
     * @return o usuário buscado, nulo se não existir
     **/
    public Usuario buscarPorEmail(String email) {
        if (!isEmailValido(email)) {
            throw new IllegalArgumentException("Email inválido.");
        }
        return usuarioRepo.findByEmail(email).orElse(null);
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
        return usuario.getCargos().stream().anyMatch(p -> p.getId().equals(papel.getId()));
    }

    public PainelHorasDTO painelHorasDTO(Integer id) {
        Usuario usuario = buscarPorId(id);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário nã existe");
        }
        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário não é discente.");
        }
        return PainelHorasDTO.builder()
                            .cargaHorariaFeita(discente.getCargaHoraria())
                            .cargaHorariaTotal(discente.getCurso().getCargaHoraria())
                            .build();
    }


}
