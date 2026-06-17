package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

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

    // ppc
    @Column(name="curriculo")
    private String curriculo;

    @OneToMany(mappedBy = "curso")
    private List<Discente> discentes;

    @OneToMany(mappedBy = "curso")
    private List<Grupo> grupos;

    @OneToMany(mappedBy = "curso")
    private List<Oportunidade> oportunidades;
}
