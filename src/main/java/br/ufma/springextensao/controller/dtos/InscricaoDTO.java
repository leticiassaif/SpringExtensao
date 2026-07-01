package br.ufma.springextensao.controller.dtos;

import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Oportunidade;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InscricaoDTO {
    String motivacao;
    Enum <Status> status;
    String justificativaCancelamento;
    LocalDate dataInscricao;
    Integer idDiscente;
    Integer idOportunidade;
}
