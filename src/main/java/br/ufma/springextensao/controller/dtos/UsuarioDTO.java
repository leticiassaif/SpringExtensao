package br.ufma.springextensao.controller.dtos;

import lombok.Data;

@Data
public class UsuarioDTO {
    String nome;
    String email;
    String senha; // nulo em response
}
