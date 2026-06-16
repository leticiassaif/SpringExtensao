package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "curso")
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    private Integer id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "codigo")
    private String codigo;
}
