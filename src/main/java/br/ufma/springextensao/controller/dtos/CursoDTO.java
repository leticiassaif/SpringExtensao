package br.ufma.springextensao.controller.dtos;

import lombok.Data;

@Data
public class CursoDTO {
    String codigo;
    String curriculo;
    Integer cargaHoraria;
    String dataInicio;
    String dataFim;
}
