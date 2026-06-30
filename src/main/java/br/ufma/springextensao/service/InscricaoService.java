package br.ufma.extensao.servicos;

import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Inscricao;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.repository.InscricaoRepo;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InscricaoService {

    private final InscricaoRepo inscricaoRepo;

    public InscricaoService(InscricaoRepo inscricaoRepository) {
        this.inscricaoRepo = inscricaoRepository;
    }

    public Inscricao inscrever(Discente discente, Oportunidade oportunidade, String motivacao) {
        if (discente == null || !discente.isAtivo() || oportunidade == null || motivacao == null) {
            throw new IllegalArgumentException("Dados obrigatórios ausentes.");
        }

        if (oportunidade.getStatus() != StatusOportunidade.ABERTA) {
            throw new IllegalStateException("Oportunidade não está aberta para inscrições.");
        }

        List<Inscricao> fila = inscricaoRepo.findByOportunidade(oportunidade);

        for (Inscricao inscricaoExistente : fila) {
            if (inscricaoExistente.getDiscente().equals(discente)) {
                throw new IllegalStateException("O usuário " + discente.getNome() + " já foi inscrito na oportunidade");
            }
        }

        //colocar geração de id

        Inscricao inscricao = new Inscricao(id, discente, oportunidade, motivacao);

        if (listarSlotsOcupados(oportunidade).size() >= oportunidade.getVagas()) {
            inscricao.setStatus(StatusInscricao.LISTA_DE_ESPERA);
        }

        return inscricaoRepo.save(inscricao);
    }

    private List<Inscricao> listarSlotsOcupados(Oportunidade oportunidade) {

        if (oportunidade == null) {
            throw new IllegalArgumentException("Campos obrigatórios não foram informados");
        }

        List<Inscricao> fila = inscricaoRepo.findByOportunidade(oportunidade);

        List<Inscricao> resultado = new ArrayList<>();

        for (Inscricao inscricao : fila) {
            if (inscricao.getStatus().equals(Status.APROVADA) || inscricao.getStatus().equals(Status.PENDENTE)) {
                resultado.add(inscricao);
            }
        }
        return resultado;
    }

    private Inscricao buscarInscricao(String inscricaoId, Oportunidade oportunidade) {
        if (oportunidade == null) {
            throw new IllegalArgumentException("Campos obrigatórios não foram informados");
        }

        if (inscricaoId == null || inscricaoId.isBlank()) {
            throw new IllegalArgumentException("O ID da Inscrição não foi informado");
        }

        Inscricao inscricao = inscricaoRepo.findById(inscricaoId)
                .orElseThrow(() -> new NoSuchElementException("A inscrição não foi achada"));

        if (!inscricao.getOportunidade().equals(oportunidade)) {
            throw new NoSuchElementException("A inscrição não pertence a essa oportunidade");
        }

        return inscricao;
    }


    public Inscricao aprovar(String inscricaoId, Oportunidade oportunidade, Usuario solicitante) {

        if (solicitante == null || !solicitante.isAtivo()) {
            throw new IllegalArgumentException("O Solicitante deve ser informado");
        }

        if (inscricaoId == null || inscricaoId.isBlank()) {
            throw new IllegalArgumentException("O ID da Inscrição não foi informado");
        }

        boolean autor = solicitante.getId().equals(oportunidade.getAutor().getId());
        boolean docenteResponsavel = solicitante.getId().equals(oportunidade.getDocenteResponsavelId());

        if (!(autor || docenteResponsavel || solicitante.getPapel().equals(Papel.ADMIN))) {
            throw new IllegalStateException("O Solicitante deve ser o responsável pela Oportunidade");
        }

        int aprovadas = 0;
        for (Inscricao i : listarSlotsOcupados(oportunidade)) {
            if (i.getStatus().equals(Status.APROVADA)) {
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

        inscricao.setStatus(Status.APROVADA);
        return inscricaoRepo.save(inscricao);
    }

    public Inscricao rejeitarRemoverDiscente(String inscricaoId, String justificativa, Oportunidade oportunidade, Usuario solicitante) {

        if (solicitante == null || !solicitante.isAtivo()) {
            throw new IllegalArgumentException("O Solicitante deve ser informado");
        }

        if (inscricaoId == null || inscricaoId.isBlank()) {
            throw new IllegalArgumentException("O ID da Inscrição não foi informado");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("A justificativa não foi informada");
        }

        boolean autor = solicitante.getId().equals(oportunidade.getAutor().getId());
        boolean docenteResponsavel = solicitante.getId().equals(oportunidade.getDocenteResponsavelId());

        if (!(autor || docenteResponsavel || solicitante.getPapel().equals(Papel.ADMIN))) {
            throw new IllegalStateException("O Solicitante deve ser o responsável pela Oportunidade");
        }

        Inscricao inscricao = buscarInscricao(inscricaoId, oportunidade);

        if (inscricao.getStatus().equals(Status.APROVADA)) {
            inscricao.setStatus(Status.CANCELADA);
        } else if (inscricao.getStatus().equals(Status.PENDENTE)) {
            inscricao.setStatus(Status.REJEITADA);
        } else {
            throw new IllegalStateException("Só é possível rejeitar inscrições PENDENTES ou remover participantes APROVADOS");
        }

        inscricao.setJustificativaCancelamento(justificativa);
        inscricaoRepo.save(inscricao);
        promoverFilaEspera(oportunidade);
        return inscricao;
    }

    public Inscricao desistir(String inscricaoId, Oportunidade oportunidade, Usuario solicitante) {

        if (inscricaoId == null || inscricaoId.isBlank()) {
            throw new IllegalArgumentException("O ID da Inscrição é inválido");
        }

        Inscricao inscricao = buscarInscricao(inscricaoId, oportunidade);

        if (inscricao.getStatus().equals(Status.REJEITADA) || inscricao.getStatus().equals(Status.CANCELADA)) {
            throw new IllegalStateException("A inscrição já está rejeitada ou cancelada");
        }

        boolean autorIsDiscente = solicitante.getId().equals(inscricao.getDiscente().getId());

        if (!autorIsDiscente) {
            throw new IllegalStateException("Apenas o próprio discente pode desistir");
        }

        inscricao.setStatus(Status.CANCELADA);
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
            primeiro.setStatus(StatusInscricao.PENDENTE);
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

        List<Inscricao> fila = inscricaoRepo.findByOportunidadeAndStatus(oportunidade, Status.LISTA_DE_ESPERA);

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