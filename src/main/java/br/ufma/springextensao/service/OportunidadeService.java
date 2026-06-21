package br.ufma.springextensao.service;

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
        return oportunidadeRepo.findById(id).orElse(null);
    }

    /**
    Essa função cria uma Oportunidade
    @param titulo nome da oportunidade
     @param descricao o que é a oportunidade
     @param cargaHoraria quantas horas a oportunidade oferece
     @param vagas quantas vagas a oportunidade oferece
     @param inicio data de inicio da oportunidade
     @param fim data de inicio da oportunidade
    @return oportunidade salvada no repo
     */
    public Oportunidade criaOportunidade(String titulo, String descricao, Integer cargaHoraria, int vagas, LocalDate inicio, LocalDate fim) {
        if (titulo == null || titulo.isBlank()){
            throw new IllegalArgumentException("Título é obrigatório.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição é obrigatória.");
        }
        if (cargaHoraria <= 0) {
            throw new IllegalArgumentException("Carga horária deve ser positiva.");
        }
        if (inicio == null || fim == null || fim.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Datas inválidas.");
        }

        // Usar a função para hasPermissao

        Oportunidade oportunidade = Oportunidade.builder()
                .titulo(titulo)
                .descricao(descricao)
                .cargaHoraria(cargaHoraria)
                .vagas(vagas)
                .dataInicio(inicio)
                .dataFim(fim)
//              .status(StatusOportunidade.RASCUNHO) esperando para ver se vai ser enum ou n
                .build();

        return oportunidadeRepo.save(oportunidade);
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

