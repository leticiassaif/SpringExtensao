package br.ufma.springextensao.controller.dtos;

import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Oportunidade;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InscricaoDTO {
    Integer id;
    String motivacao;
    Enum <Status> status;
    String justificativaCancelamento;
    LocalDate dataInscricao;
    Discente discente;
    Oportunidade oportunidade;
}
