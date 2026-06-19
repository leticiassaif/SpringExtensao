package br.ufma.springextensao.controller.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocenteDTO extends UsuarioDTO {
    String siape;
    String departamento;
}
