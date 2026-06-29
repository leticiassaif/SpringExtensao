package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.OportunidadeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
public class OportunidadeService {

    @Autowired
    private OportunidadeRepo oportunidadeRepo;

    public Oportunidade buscarOportunidadePorId(Integer id) {
        if (id == null)
            throw new IllegalArgumentException("ID é obrigatório.");
        return oportunidadeRepo.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Oportunidade não encontrada!"));
    }

    /**
    Essa função cria uma Oportunidade
    @return oportunidade salva no repo
     */

    public Oportunidade criaOportunidade(OportunidadeDTO oportunidade) {
        if (oportunidade.getTitulo() == null || oportunidade.getTitulo().isBlank()){
            throw new IllegalArgumentException("Título é obrigatório.");
        }
        if (oportunidade.getDescricao() == null || oportunidade.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descrição é obrigatória.");
        }
        if (oportunidade.getCargaHoraria() <= 0) {
            throw new IllegalArgumentException("Carga horária deve ser positiva.");
        }

        LocalDate inicio = oportunidade.getDataInicio();
        LocalDate fim = oportunidade.getDataFim();

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Datas de início e fim são obrigatórias.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Data de fim não pode ser antes do início.");
        }
        if (inicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data de início não pode ser no passado.");
        }

        // Usar a função para hasPermissao

        Oportunidade nova = Oportunidade.builder()
                .titulo(oportunidade.getTitulo())
                .descricao(oportunidade.getDescricao())
                .cargaHoraria(oportunidade.getCargaHoraria())
                .vagas(oportunidade.getVagas())
                .dataInicio(inicio)
                .dataFim(oportunidade.getDataFim())
                .status(StatusOp.RASCUNHO)
                .build();

        return oportunidadeRepo.save(nova);
    }

    /**
     * Essa função faz a lógica de publicacao de um Oportunidade, mudadando o seu status.
     * @param idOportunidade id da oportunidade
     * @param solicitante usuario que fez a solicitacao
     * @return oportunidade atualizada como Aguardando aprovação ou Aberta
     */
    public Oportunidade publicarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);
        if (oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

//        if (UsuarioService.hasPermissao(solicitante, papelDiscenteDiretor)) {
//            oportunidade.setStatus(StatusOportunidade.AGUARDANDO_APROVACAO);
//        } else if (UsuarioService.hasPermissao(solicitante, papelDocente) || UsuarioService.hasPermissao(solicitante, papelDocente) ) {
//            oportunidade.setStatus(StatusOportunidade.ABERTA);
//        } else {
//            throw new IllegalArgumentException("Usuário não tem permissão para publicar.");
//        }

        return oportunidadeRepo.save(oportunidade);
    }


    /**
     * Essa função faz a aprovação de uma oportunidade já criada
     * @param idOportunidade id da oportunidade
     * @param solicitante usuario que fez a solicitacao
     * @return oportunidade salva com novo status, ou null se não condizer com as regras de negocio
     */
    public Oportunidade aprovarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if(oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

//        if (UsuarioService.hasPermissao(solicitante, papelDocente) || UsuarioService.hasPermissao(solicitante, papelAdmin) ) {
//                if (oportunidade.getStatus == StatusOportunidade.AGUARDANDO_APROVACAO) {
//                    oportunidade.setStatus(StatusOportunidade.ABERTA);
//                    return oportunidadeRepo.save(oportunidade);
//                }
//        } else {
//            throw new IllegalArgumentException("Usuário não tem permissão para publicar.");

        return null;
    }

    /**
     * Essa função muda o status de uma oportunidade de "aberta" para "em execucao"
     * @param idOportunidade
     * @param solicitante
     * @return oportunidade salva com novo status, ou null se não condizer com as regras de negocio
     */
    public Oportunidade iniciarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if(oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

//        if (UsuarioService.hasPermissao(solicitante, papelDocente) || UsuarioService.hasPermissao(solicitante, papelAdmin) ) {
//                if (oportunidade.getStatus == StatusOportunidade.ABERTA) {
//                    oportunidade.setStatus(StatusOportunidade.EM_EXECUCAO);
//                    return oportunidadeRepo.save(oportunidade);
//                }
//        } else {
//            throw new IllegalArgumentException("Usuário não tem permissão para publicar.");

        return null;
    }

    /**
     *  Essa função muda o status de uma oportunidade de "em execucao" para "encerrada"
     * @param idOportunidade
     * @param solicitante
     * @return oportunidade salva com novo status, ou null se não condizer com as regras de negocio
     */
    public Oportunidade encerrarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if(oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

//        if (UsuarioService.hasPermissao(solicitante, papelDocente) || UsuarioService.hasPermissao(solicitante, papelAdmin) ) {
//                if (oportunidade.getStatus == StatusOportunidade.EM_EXECUCAO) {
//                    oportunidade.setStatus(StatusOportunidade.ENCERRADA);
//                    return oportunidadeRepo.save(oportunidade);
//                }
//        } else {
//            throw new IllegalArgumentException("Usuário não tem permissão para publicar.");

        return null;
    }

    /**
     *  Essa função muda o status de uma oportunidade para "cancelada"
     * @param idOportunidade
     * @param solicitante
     * @return oportunidade salva com novo status, ou null se não condizer com as regras de negocio
     */
    public Oportunidade cancelarOportunidade(Integer idOportunidade, Usuario solicitante) {
        Oportunidade oportunidade = buscarOportunidadePorId(idOportunidade);

        if(oportunidade == null) {
            throw new IllegalArgumentException("Oportunidade não encontrada.");
        }

//        if (UsuarioService.hasPermissao(solicitante, papelDocente) || UsuarioService.hasPermissao(solicitante, papelAdmin) {
//                    oportunidade.setStatus(StatusOportunidade.ENCERRADA);
//                    return oportunidadeRepo.save(oportunidade);
//                }
//        } else {
//            throw new IllegalArgumentException("Usuário não tem permissão para publicar.");

        return null;
    }

    /**
     * essa função faz a listagem de todas as oportunidades
     * @return lista com todas as oportunidades encontradas no repositorio
     */
    public List<Oportunidade> listarOportunidades() {
        return oportunidadeRepo.findAll();
    }

}

