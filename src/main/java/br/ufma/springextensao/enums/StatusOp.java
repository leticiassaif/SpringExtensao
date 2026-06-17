package br.ufma.springextensao.enums;

import lombok.Getter;

@Getter
public enum StatusOp {
    ABERTA("Aberta"),
    RASCUNHO("Rascunho"),
    ENCERRADA("Encerrada"),
    CANCELADA("Cancelada"),
    AGUARDA_APROVACAO("Aguarda aprovação"),
    EM_EXECUCAO("Em execução");

    private final String descricao;

    StatusOp (String descricao) {
        this.descricao = descricao;
    }
}
