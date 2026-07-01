package br.ufma.springextensao.controller.dtos;

import br.ufma.springextensao.enums.StatusOp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class OportunidadeDTO {
    String titulo;
    String descricao;
    LocalDate dataInicio;
    LocalDate dataFim;
    Enum<StatusOp> status;
    String tipo;
    Integer vagas;
    Integer cargaHoraria;
    Integer idDocente;
}
