package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "oportunidade")
public class Oportunidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "titulo")
    private String titulo;

    private String descricao;

    @Column(name = "tipo")
    private Tipo tipo;
    // modalidade (enum)

    @Column(name = "carga_horaria")
    private int cargaHoraria;

    @Column(name = "vagas")
    private int vagas;
    // status
    // docente

    //TODO: resolver mapeamento de oportunidade
}
