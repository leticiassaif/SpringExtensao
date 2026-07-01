package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.OportunidadeRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static br.ufma.springextensao.service.UsuarioService.hasPermissao;


@Service
public class OportunidadeService {
    @Autowired
    private OportunidadeRepo oportunidadeRepo;

    @Autowired
    PapelRepo papelRepo;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    UsuarioRepo usuarioRepo;

    public Oportunidade buscarOportunidadePorId(Integer id) {
        return oportunidadeRepo.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Oportunidade não encontrada!"));
    }

    /**
     * Essa função cria uma nova oportunidade
     * @param oportunidade objeto para transferir informação
     * @return oportunidade persistida no banco
     **/
    public Oportunidade criaOportunidade(Usuario solicitante, OportunidadeDTO oportunidade) {
        Papel diretor = papelRepo.findByNome("DIRETOR");

        if (!hasPermissao(solicitante, diretor) && !(solicitante instanceof Docente)) {
            throw new SecurityException("O solicitante não possui permissão.");
        }

    public Oportunidade criaOportunidade(OportunidadeDTO oportunidade, Usuario solicitante) {
        Papel diretor = papelRepo.findByNome("DIRETOR");

        if (oportunidade.getTitulo() == null || oportunidade.getTitulo().isBlank()){
            throw new IllegalArgumentException("Título é obrigatório.");
        }
        if (oportunidade.getDescricao() == null || oportunidade.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição é obrigatória.");
        }
        if (oportunidade.getCargaHoraria() <= 0) {
            throw new IllegalArgumentException("Carga horária deve ser positiva.");
        }

        Usuario usuario = usuarioService.buscarPorId(oportunidade.getIdDocente());

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Docente docente) 
        if (UsuarioService.hasPermissao(solicitante, docente) || !hasPermissao(solicitante, diretor)) {
            throw new IllegalArgumentException("Usuário não tem permissão.");
        }

        Oportunidade nova = Oportunidade.builder()
                .titulo(oportunidade.getTitulo())
                .descricao(oportunidade.getDescricao())
                .cargaHoraria(oportunidade.getCargaHoraria())
                .vagas(oportunidade.getVagas())
                .status(StatusOp.RASCUNHO)
                .coordenador(docente)
                .build();

    }

    /**
     * Essa função faz a lógica de publicação de um Oportunidade, mudando o seu status.
     * @param idOportunidade id da oportunidade
     * @param solicitante usuário que fez a solicitação
     * @return oportunidade persistida no banco
     */
    public Oportunidade publicarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Papel diretor = papelRepo.findByNome("DIRETOR");
        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

        if (!hasPermissao(solicitante, diretor) && !(solicitante instanceof Docente)) {
            throw new SecurityException("O solicitante não possui permissão.");
        }

        if (solicitante instanceof Docente) {
            oportunidade.setStatus(StatusOp.ABERTA);
        } else {
            oportunidade.setStatus(StatusOp.AGUARDA_APROVACAO);
        }

        return oportunidadeRepo.save(oportunidade);
    }


    /**
     * Essa função faz a aprovação de uma oportunidade já criada
     * @param idOportunidade id da oportunidade
     * @param solicitante usuário que fez a solicitação
     * @return oportunidade persistida no banco
     */
    public Oportunidade aprovarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Papel admin = papelRepo.findByNome("ADMIN");

        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if (!(solicitante instanceof Docente) || !hasPermissao(solicitante, admin)) {
            throw new SecurityException("O solicitante não possui permissão.");
        }

        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

        if (oportunidade.getStatus() != StatusOp.AGUARDA_APROVACAO) {
            throw new IllegalStateException("Oportunidade deve está aguardando aprovação.");
        }

        oportunidade.setStatus(StatusOp.ABERTA);

        return oportunidadeRepo.save(oportunidade);
    }

    /**
     * Essa função muda o status de uma oportunidade de "aberta" para "em execucao"
     * @param idOportunidade id da oportunidade
     * @param solicitante usuário que fez a solicitação
     * @return oportunidade persistida no banco
     */
    public Oportunidade iniciarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Papel admin = papelRepo.findByNome("ADMIN");

        if (!(solicitante instanceof Docente) && !hasPermissao(solicitante, admin)) {
            throw new SecurityException("O solicitante não possui permissão.");
        }

        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

        if (oportunidade.getStatus() != StatusOp.ABERTA) {
            throw new IllegalStateException("Oportunidade deve está aberta.");
        }

        oportunidade.setStatus(StatusOp.EM_EXECUCAO);
        oportunidade.setDataInicio(LocalDate.now());

        return oportunidadeRepo.save(oportunidade);
    }

    /**
     *  Essa função muda o status de uma oportunidade de "em excecução" para "encerrada"
     * @param idOportunidade id da oportunidade
     * @param solicitante usuário que fez a solicitação
     * @return oportunidade persistida no banco
     */
    public Oportunidade encerrarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Papel admin = papelRepo.findByNome("ADMIN");

        if (!(solicitante instanceof Docente) && !hasPermissao(solicitante, admin)) {
            throw new SecurityException("O solicitante não possui permissão.");
        }

        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if(oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

        if (oportunidade.getStatus() != StatusOp.EM_EXECUCAO) {
            throw new IllegalStateException("Oportunidade deve está em execução.");
        }

        oportunidade.setStatus(StatusOp.ENCERRADA);
        oportunidade.setDataFim(LocalDate.now());

        oportunidade.getDiscentesOp().forEach(
                d -> d.setCargaHoraria(d.getCargaHoraria() + oportunidade.getCargaHoraria()));

        usuarioRepo.saveAll(oportunidade.getDiscentesOp());
        return oportunidadeRepo.save(oportunidade);
    }

    /**
     *  Essa função muda o status de uma oportunidade para "cancelada"
     * @param idOportunidade id da oportunidade
     * @param solicitante usuário que fez a solicitação
     * @return oportunidade persistida no banco
     */
    public Oportunidade cancelarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Papel admin = papelRepo.findByNome("ADMIN");

        if (!(solicitante instanceof Docente) && !hasPermissao(solicitante, admin)) {
            throw new SecurityException("O solicitante não possui permissão.");
        }

        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if(oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

        oportunidade.setStatus(StatusOp.CANCELADA);

    }

    /**
     * Essa função faz a listagem de todas as oportunidades
     * @return lista com todas as oportunidades encontradas no repositório
     */
    public List<Oportunidade> listarOportunidades() {
        return oportunidadeRepo.findAll();
    }

}

