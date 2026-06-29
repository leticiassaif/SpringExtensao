package br.ufma.springextensao.controller.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DiscenteDTO extends UsuarioDTO {
    String matricula;
    Integer cargaHoraria;
    Integer idCurso;
}
