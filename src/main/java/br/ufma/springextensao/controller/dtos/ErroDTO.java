package br.ufma.springextensao.controller.dtos;

import lombok.Getter;

@Getter
public class ErroDTO {
    private Integer status;
    private String erro;
    private String mensagem;

    public ErroDTO(Integer status, String erro, String mensagem) {
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
    }
}
