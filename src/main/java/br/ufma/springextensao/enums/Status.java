package br.ufma.springextensao.enums;

import lombok.Getter;

@Getter
public enum Status {
    APROVADO("Aprovado"),
    PENDENTE("Pendente"),
    INDEFERIDO("Indeferido"),
    CANCELADO("Cancelado"),
    REJEITADO("Rejeitado");

    private final String descricao;

    Status (String descricao) {
        this.descricao = descricao;
    }
}
