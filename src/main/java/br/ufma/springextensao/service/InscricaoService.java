package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.repository.InscricaoRepo;
import br.ufma.springextensao.repository.OportunidadeRepo;
import br.ufma.springextensao.repository.PapelRepo;

import br.ufma.springextensao.repository.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class InscricaoService {

    @Autowired
    InscricaoRepo inscricaoRepo;

    @Autowired
    PapelRepo papelRepo;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    OportunidadeService oportunidadeService;

    @Autowired
    OportunidadeRepo oportunidadeRepo;

    @Autowired
    UsuarioRepo usuarioRepo;

    /**
     *  Essa função permite uma pessoa se inscrever em uma oportunidade
     * @param inscricao objeto de transferência
     * @return inscrição persistida no banco
     */
    public Inscricao inscrever(InscricaoDTO inscricao) {
        Usuario usuario = usuarioService.buscarPorId(inscricao.getIdDiscente());
        Oportunidade oportunidade = oportunidadeService.buscarOportunidadePorId(inscricao.getIdOportunidade());

        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não existente");
        }

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existente");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário precisa ser discente.");
        }

        if (inscricao.getMotivacao() == null || inscricao.getMotivacao().isBlank()) {
            throw new IllegalArgumentException("Motivação ausentes.");
        }

        if (oportunidade.getStatus() != StatusOp.ABERTA) {
            throw new IllegalStateException("Oportunidade não está aberta para inscrições.");
        }

        Inscricao existente = inscricaoRepo.findByDiscenteAndOportunidade(discente, oportunidade).orElse(null);

        if (existente != null) {
            throw new IllegalStateException("O usuário " + discente + " já foi inscrito na oportunidade");
        }

        Inscricao nova = Inscricao.builder()
                .motivacao(inscricao.getMotivacao())
                .status(Status.PENDENTE)
                .dataInscricao(LocalDate.now())
                .discente(discente)
                .oportunidade(oportunidade)
                .build();

        return inscricaoRepo.save(nova);
    }

    /**
     *  Essa função lista os slots ocupados
     * @param oportunidade objeto
     * @return oportunidade persistida no banco
     */
    private List<Inscricao> listarSlotsOcupados(Oportunidade oportunidade) {

        if (oportunidade == null) {
            throw new IllegalArgumentException("Campos obrigatórios não foram informados");
        }

        List<Inscricao> fila = inscricaoRepo.findByOportunidade(oportunidade);

        List<Inscricao> resultado = new ArrayList<>();

        for (Inscricao inscricao : fila) {
            if (inscricao.getStatus().equals(Status.APROVADO) || inscricao.getStatus().equals(Status.PENDENTE)) {
                resultado.add(inscricao);
            }
        }
        return resultado;
    }

    /**
     *  Essa função aprova uma inscrição
     * @param id id da inscrição
     * @param solicitante quem chamou a função
     * @return inscrição persistida no banco
     */
    public Inscricao aprovar(Integer id, Usuario solicitante) {
        if (solicitante == null) {
            throw new IllegalArgumentException("O solicitante deve ser informado");
        }

        if (!(solicitante instanceof Docente)) {
            throw new IllegalArgumentException("O solicitante precisa ser docente");
        }

        Inscricao inscricao = buscaPorId(id);

        if (inscricao == null) {
            throw new IllegalArgumentException("Inscrição não existe");
        }

        if (!(inscricao.getStatus().equals(Status.PENDENTE))) {
            throw new IllegalStateException("Só é possível aprovar inscrições pendentes.");
        }

        Oportunidade oportunidade = inscricao.getOportunidade();
        Discente discente = inscricao.getDiscente();

        if (oportunidade.getVagasLivres() <= 0)  {
            throw new IllegalStateException("A oportunidade não possui vagas livres.");
        }

        inscricao.setStatus(Status.APROVADO);
        oportunidadeService.diminuiVagasLivres(oportunidade);
        oportunidade.getDiscentesOp().add(discente);
        discente.getOportunidades().add(oportunidade);

        usuarioRepo.save(discente);
        oportunidadeRepo.save(oportunidade);
        return inscricaoRepo.save(inscricao);
    }

    /**
     *  Essa função rejeita uma inscrição
     * @param id id da inscrição
     * @param justificativa justificativa da rejeição
     * @param solicitante quem chamou a função
     * @return inscrição persistida no banco
     */
    public Inscricao rejeitar(Integer id, String justificativa, Usuario solicitante) {
        if (solicitante == null || !solicitante.isAtivo()) {
            throw new IllegalArgumentException("O solicitante deve ser informado");
        }

        Inscricao inscricao = buscaPorId(id);

        if (inscricao == null) {
            throw new IllegalArgumentException("Inscrição não existe.");
        }

        Oportunidade oportunidade = inscricao.getOportunidade();

        if (!solicitante.getId().equals(oportunidade.getCoordenador().getId())) {
            throw new SecurityException("Apenas o responsável pode fazer isso.");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("A justificativa não foi informada");
        }

        if (inscricao.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Só é possível rejeitar inscrições PENDENTES.");
        }

        inscricao.setStatus(Status.REJEITADO);
        inscricao.setJustificativaCancelamento(justificativa);

        return inscricaoRepo.save(inscricao);
    }

    /**
     *  Essa função remove um discente da oportunidade por meio da inscrição
     * @param id id da inscrição
     * @param justificativa justificativa da remoção
     * @param solicitante quem chamou a função
     * @return inscrição persistida no banco
     */
    public Inscricao removerDiscente(Integer id, String justificativa, Usuario solicitante) {
        if (solicitante == null || !solicitante.isAtivo()) {
            throw new IllegalArgumentException("O solicitante deve ser informado");
        }

        Inscricao inscricao = buscaPorId(id);

        if (inscricao == null) {
            throw new IllegalArgumentException("Inscrição não existe.");
        }

        Oportunidade oportunidade = inscricao.getOportunidade();
        Discente discente = inscricao.getDiscente();

        if (!solicitante.getId().equals(oportunidade.getCoordenador().getId())) {
            throw new SecurityException("Apenas o responsável pode fazer isso.");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("A justificativa não foi informada");
        }

        if (inscricao.getStatus() != Status.PENDENTE) {
            throw new IllegalStateException("Só é possível retirar inscrições aprovadas.");
        }

        inscricao.setStatus(Status.CANCELADO);
        inscricao.setJustificativaCancelamento(justificativa);

        oportunidade.getDiscentesOp().remove(discente);
        oportunidadeService.aumentarVagasLivres(oportunidade);
        discente.getOportunidades().remove(oportunidade);

        oportunidadeRepo.save(oportunidade);
        usuarioRepo.save(discente);
        return inscricaoRepo.save(inscricao);
    }

    /**
     *  Essa função remove um discente da oportunidade por meio da inscrição
     * @param id id da inscrição
     * @param solicitante quem chamou a função
     * @return inscrição persistida no banco
     */
    public Inscricao desistir(Integer id, Usuario solicitante) {
        Inscricao inscricao = buscaPorId(id);

        if (inscricao == null) {
            throw new IllegalArgumentException("Inscrição não existe.");
        }

        if (inscricao.getStatus().equals(Status.REJEITADO) || inscricao.getStatus().equals(Status.CANCELADO)) {
            throw new IllegalStateException("A inscrição já está rejeitada ou cancelada");
        }

        boolean autorIsDiscente = solicitante.getId().equals(inscricao.getDiscente().getId());

        if (!autorIsDiscente) {
            throw new IllegalStateException("Apenas o próprio discente pode desistir");
        }

        if (inscricao.getStatus() == Status.APROVADO) {
            Oportunidade oportunidade = inscricao.getOportunidade();
            Discente discente = inscricao.getDiscente();

            oportunidade.getDiscentesOp().remove(discente);
            oportunidadeService.aumentarVagasLivres(oportunidade);
            discente.getOportunidades().remove(oportunidade);

            oportunidadeRepo.save(oportunidade);
            usuarioRepo.save(discente);
        }

        inscricao.setStatus(Status.CANCELADO);
        inscricao.setJustificativaCancelamento("O Discente desistiu da vaga");
        inscricaoRepo.save(inscricao);
        return inscricao;
    }


    /**
     *  Essa função lista as inscrições de uma oportunidade
     * @param id id da oportunidade
     * @return lista de inscrição, vazia caso não existam
     */
    public List<Inscricao> listarPorOportunidade(Integer id) {
        Oportunidade oportunidade = oportunidadeService.buscarOportunidadePorId(id);
        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não existe.");
        }
        return inscricaoRepo.findByOportunidade(oportunidade);
    }

    /**
     *  Essa função lista as inscrições na fila de espera de uma oportunidade
     * @param id id da oportunidade
     * @return lista de inscrição, vazia caso não existam
     */
    public List<Inscricao> listarFilaEspera(Integer id) {
        Oportunidade oportunidade = oportunidadeService.buscarOportunidadePorId(id);
        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não existe.");
        }
        return inscricaoRepo.findByOportunidadeAndStatus(oportunidade, Status.PENDENTE);
    }

    /**
     *  Essa função lista as inscrições de um discente
     * @param id id do discente
     * @return lista de inscrição, vazia caso não existam
     */
    public List<Inscricao> listarPorDiscente(Integer id) {
        Usuario usuario = usuarioService.buscarPorId(id);

        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não existe.");
        }

        if (!(usuario instanceof Discente discente)) {
            throw new IllegalArgumentException("Usuário precisa ser discente.");
        }

        return inscricaoRepo.findByDiscente(discente);
    }

    /**
     *  Essa função busca uma inscrição
     * @param id id da inscrição
     * @return inscrição, nulo caso não exista
     */
    public Inscricao buscaPorId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return inscricaoRepo.findById(id).orElse(null);
    }
}