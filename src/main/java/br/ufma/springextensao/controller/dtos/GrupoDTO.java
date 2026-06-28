package br.ufma.springextensao.controller.dtos;

import lombok.Data;

@Data
public class GrupoDTO {
    String nome;
    String descricao;
    String email;
    Integer idCurso;
    Integer idDiretor;
    Integer idResponsavel;
}
