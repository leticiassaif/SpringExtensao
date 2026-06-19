package br.ufma.springextensao.controller.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SolicitacaoDTO {
    String descricao;
    Integer cargaHoraria;
    String dataSolicitacao;
    Integer idDiscente;
}
