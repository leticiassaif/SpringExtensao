package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.CursoDTO;
import br.ufma.springextensao.model.Curso;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static br.ufma.springextensao.service.UsuarioService.hasPermissao;
import static br.ufma.springextensao.util.Validacao.formataDataIso;

@Service
public class CursoService {
    @Autowired
    CursoRepo cursoRepo;

    @Autowired
    PapelRepo papelRepo;

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
    public Curso cadastraCurso(Usuario solicitante, CursoDTO curso) {
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

    public Curso buscaPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return cursoRepo.findById(id).orElse(null);
    }
}
