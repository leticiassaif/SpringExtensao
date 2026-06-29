package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.CursoDTO;
import br.ufma.springextensao.controller.dtos.UCEDTO;
import br.ufma.springextensao.model.Curso;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.UCE;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UCERepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static br.ufma.springextensao.service.UsuarioService.hasPermissao;
import static br.ufma.springextensao.util.Validacao.formataDataIso;

@Service
public class CursoService {
    @Autowired
    CursoRepo cursoRepo;

    @Autowired
    UCERepo uceRepo;

    @Autowired
    PapelRepo papelRepo;

    // verificar necessidade
    /**
     * Essa função cria um novo curso (PPC) vigente
     * @param solicitante quem chamou a função
     * @param curso objeto para transferir informação
     * @return curso/ppc persistido no banco
     **/
    @Transactional
    public Curso criarCurso(Usuario solicitante, CursoDTO curso) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)) {
            throw new SecurityException("O solicitante não possui permissão para criar curso/ppc.");
        }

        Curso c = Curso.builder().
                codigo(curso.getCodigo()).
                curriculo(curso.getCurriculo()).
                build();

        Curso anterior = cursoRepo.findVigente();
        if (anterior != null) {
            anterior.setDataFim(LocalDate.now());
            cursoRepo.save(anterior);
        }

        c.setDataInicio(LocalDate.now());
        return cursoRepo.save(c);
    }

    /**
     * Essa função cadastra um novo curso (PPC) para ficar no histórico
     * @param solicitante quem chamou a função
     * @param curso objeto para transferir informação
     * @return curso/ppc persistido no banco
     **/
    @Transactional
    public Curso cadastrarCurso(Usuario solicitante, CursoDTO curso) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)) {
            throw new SecurityException("O solicitante não possui permissão para criar curso/ppc.");
        }

        LocalDate inicio = formataDataIso(curso.getDataInicio());
        LocalDate fim = formataDataIso(curso.getDataFim());

        Curso c = Curso.builder().
                codigo(curso.getCodigo()).
                curriculo(curso.getCurriculo()).
                build();


        c.setDataInicio(inicio);
        c.setDataFim(fim);

        return cursoRepo.save(c);
    }

    /**
     * Essa função busca um curso (PPC) por sua versão
     * @param versao versão do ppc em questão
     * @return curso/ppc procurado, nulo caso não exista
     **/
    public Curso buscarPorVersao(String versao) {
        if (versao == null) {
            throw new IllegalArgumentException("Versão inválida");
        }
        return cursoRepo.findByVersao(versao).orElse(null);
    }

    /**
     * Essa função busca o curso (PPC) vigente
     * @return curso/ppc vigente
     **/
    public Curso buscarVigente() {
        return cursoRepo.findVigente();
    }

    /**
     * Essa função busca o curso (PPC) por seu id
     * @return curso/ppc procurado, nulo caso não exista
     **/
    public Curso buscaPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return cursoRepo.findById(id).orElse(null);
    }

    /**
     * Essa função lista o histórico dos cursos (PPC)
     * @return lista com todos os cursos/ppcs cadastrados
     **/
    public List<Curso> listaHistorico() {
        return cursoRepo.findAll();
    }

    /**
     * Essa função busca o curso (PPC) vigente
     * @param solicitante quem chamou a função
     * @param uce objeto para transferir informação
     * @return uce persistida no banco
     **/
    public UCE cadastrarUCE(Usuario solicitante, UCEDTO uce) {
        Papel admin = papelRepo.findByNome("ADMIN");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (!hasPermissao(solicitante, admin) && !hasPermissao(solicitante, coordenador)) {
            throw new SecurityException("O solicitante não possui permissão para criar curso/ppc.");
        }

        Curso curso = buscaPorId(uce.getIdCurso());

        if (curso == null) {
            throw new IllegalArgumentException("Curso não existe");
        }

        UCE u = UCE.builder().
                nome(uce.getNome()).
                cargaHoraria(uce.getCargaHoraria()).
                curso(curso).
                build();

        return uceRepo.save(u);
    }

    /**
     * Essa função busca um curso (PPC) por sua versão
     * @param id id do curso/ppc desejado
     * @return lista das uces procuradas, lista vazia caso não exista
     **/
    public List<UCE> buscaUCEPorPPC(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }

        Curso curso = buscaPorId(id);

        if (curso == null) {
            throw new IllegalArgumentException("Curso não existe.");
        }

        return uceRepo.findByCurso(curso);
    }
}
