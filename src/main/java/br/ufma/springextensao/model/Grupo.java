package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "grupo")
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome")
    private String nome;

    private String descricao;

    @Column(name = "email")
    private String email;
    // private docente responsavel + column
    // private discente solicitante
    private String justificativaNegacao;
    // private Status status; + column
}
