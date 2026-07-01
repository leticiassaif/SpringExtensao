package br.ufma.extensao.servicos;

import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Inscricao;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.repository.InscricaoRepo;
import br.ufma.springextensao.repository.PapelRepo;

import br.ufma.springextensao.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InscricaoService {

    private final InscricaoRepo inscricaoRepo;
    private final PapelRepo papelRepo;

    public InscricaoService(InscricaoRepo inscricaoRepository, PapelRepo papelRepo) {
        this.inscricaoRepo = inscricaoRepository;
        this.papelRepo = papelRepo;
    }

    public Inscricao inscrever(InscricaoDTO inscricao) {

        if (inscricao.getDiscente() == null || inscricao.getOportunidade() == null || inscricao.getMotivacao() == null) {
            throw new IllegalArgumentException("Dados obrigatórios ausentes.");
        }

        if (inscricao.getOportunidade().getStatus() != StatusOp.ABERTA) {
            throw new IllegalStateException("Oportunidade não está aberta para inscrições.");
        }

        List<Inscricao> fila = inscricaoRepo.findByOportunidade(inscricao.getOportunidade());

        for (Inscricao inscricaoExistente : fila) {
            if (inscricaoExistente.getDiscente().equals(inscricao.getDiscente())) {
                throw new IllegalStateException("O usuário " + inscricao.getDiscente().getNome() + " já foi inscrito na oportunidade");
            }
        }

        //colocar geração de id

        Inscricao nova = Inscricao.builder()
                .motivacao(inscricao.getMotivacao())
                .status(Status.PENDENTE)
                .justificativaCancelamento(inscricao.getJustificativaCancelamento())
                .dataInscricao(inscricao.getDataInscricao())
                .discente(inscricao.getDiscente())
                .oportunidade(inscricao.getOportunidade())
                .build();

        if (listarSlotsOcupados(inscricao.getOportunidade()).size() >= inscricao.getOportunidade().getVagas()) {
            nova.setStatus(Status.ESPERA);
        }

        return inscricaoRepo.save(nova);
    }

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

    private Inscricao buscarInscricao(Integer inscricaoId, Oportunidade oportunidade) {
        if (oportunidade == null) {
            throw new IllegalArgumentException("Campos obrigatórios não foram informados");
        }

        if (inscricaoId == null || inscricaoId.describeConstable().isEmpty()) {
            throw new IllegalArgumentException("O ID da Inscrição não foi informado");
        }

        Inscricao inscricao = inscricaoRepo.findById(inscricaoId)
                .orElseThrow(() -> new NoSuchElementException("A inscrição não foi achada"));

        if (!inscricao.getOportunidade().equals(oportunidade)) {
            throw new NoSuchElementException("A inscrição não pertence a essa oportunidade");
        }

        return inscricao;
    }

    public Inscricao aprovar(Integer inscricaoId, Oportunidade oportunidade, Usuario solicitante) {

        if (solicitante == null || !solicitante.isAtivo()) {
            throw new IllegalArgumentException("O Solicitante deve ser informado");
        }

        if (inscricaoId == null) {
            throw new IllegalArgumentException("O ID da Inscrição não foi informado");
        }

        Papel docente = papelRepo.findByNome("DOCENTE");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (UsuarioService.hasPermissao(solicitante, coordenador) || UsuarioService.hasPermissao(solicitante, docente)) {
            int aprovadas = 0;
            for (Inscricao i : listarSlotsOcupados(oportunidade)) {
                if (i.getStatus().equals(Status.APROVADO)) {
                    aprovadas++;
                }
            }

            if (aprovadas >= oportunidade.getVagas()) {
                throw new IllegalStateException("Vagas esgotadas");
            }

            Inscricao inscricao = buscarInscricao(inscricaoId, oportunidade);

            if (!inscricao.getStatus().equals(Status.PENDENTE)) {
                throw new IllegalStateException("Só é possível aprovar inscrições PENDENTES");
            }

            inscricao.setStatus(Status.APROVADO);
            return inscricaoRepo.save(inscricao);
        }
        throw new IllegalArgumentException("Sem perminssão para aprovar inscrições!");
    }

    public Inscricao rejeitarRemoverDiscente(Integer inscricaoId, String justificativa, Oportunidade oportunidade, Usuario solicitante) {

        if (solicitante == null || !solicitante.isAtivo()) {
            throw new IllegalArgumentException("O Solicitante deve ser informado");
        }

        if (inscricaoId == null ) {
            throw new IllegalArgumentException("O ID da Inscrição não foi informado");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("A justificativa não foi informada");
        }

        Papel docente = papelRepo.findByNome("DOCENTE");
        Papel coordenador = papelRepo.findByNome("COORDENADOR");

        if (UsuarioService.hasPermissao(solicitante, coordenador) || UsuarioService.hasPermissao(solicitante, docente)) {
            Inscricao inscricao = buscarInscricao(inscricaoId, oportunidade);

            if (inscricao.getStatus().equals(Status.APROVADO)) {
                inscricao.setStatus(Status.CANCELADO);
            } else if (inscricao.getStatus().equals(Status.PENDENTE)) {
                inscricao.setStatus(Status.REJEITADO);
            } else {
                throw new IllegalStateException("Só é possível rejeitar inscrições PENDENTES ou remover participantes APROVADOS");
            }

            inscricao.setJustificativaCancelamento(justificativa);
            inscricaoRepo.save(inscricao);
            promoverFilaEspera(oportunidade);
            return inscricao;
        }


        throw new IllegalStateException("O Solicitante deve ser o responsável pela Oportunidade");
    }

    public Inscricao desistir(Integer inscricaoId, Oportunidade oportunidade, Usuario solicitante) {

        if (inscricaoId == null) {
            throw new IllegalArgumentException("O ID da Inscrição é inválido");
        }

        Inscricao inscricao = buscarInscricao(inscricaoId, oportunidade);

        if (inscricao.getStatus().equals(Status.REJEITADO) || inscricao.getStatus().equals(Status.CANCELADO)) {
            throw new IllegalStateException("A inscrição já está rejeitada ou cancelada");
        }

        boolean autorIsDiscente = solicitante.getId().equals(inscricao.getDiscente().getId());

        if (!autorIsDiscente) {
            throw new IllegalStateException("Apenas o próprio discente pode desistir");
        }

        inscricao.setStatus(Status.CANCELADO);
        inscricao.setJustificativaCancelamento("O Discente desistiu da vaga");
        inscricaoRepo.save(inscricao);
        promoverFilaEspera(oportunidade);
        return inscricao;
    }

    private void promoverFilaEspera(Oportunidade oportunidade) {

        if (oportunidade == null) {
            throw new IllegalArgumentException("A Oportunidade não existe");
        }

        List<Inscricao> espera = listarFilaEspera(oportunidade);

        if (!espera.isEmpty()) {
            Inscricao primeiro = espera.get(0);
            primeiro.setStatus(Status.PENDENTE);
            inscricaoRepo.save(primeiro);
        }
    }


    public List<Inscricao> listarPorOportunidade(Oportunidade oportunidade) {
        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade é obrigatória");
        }

        return inscricaoRepo.findByOportunidade(oportunidade);
    }

    public List<Inscricao> listarFilaEspera(Oportunidade oportunidade) {
        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade é obrigatória");
        }

        List<Inscricao> fila = inscricaoRepo.findByOportunidadeAndStatus(oportunidade, Status.ESPERA);

        fila.sort(Comparator.comparing(Inscricao::getDataInscricao));

        return fila;
    }

    public List<Inscricao> listarPorDiscente(Discente discente) {
        if (discente == null || !discente.isAtivo()) {
            throw new IllegalArgumentException("Discente é obrigatório");
        }

        return inscricaoRepo.findByDiscente(discente);
    }
}