package br.ufma.springextensao.service;

import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.repository.OportunidadeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
public class OportunidadeService {

    @Autowired
    private OportunidadeRepo oportunidadeRepo;

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
                .build();

        return oportunidadeRepo.save(oportunidade);
    }



}

