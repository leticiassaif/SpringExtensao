package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "discente")
@PrimaryKeyJoinColumn(name = "id_usuario")
@Data
public class Discente extends Usuario {
    @Column(name = "matricula")
    private String matricula;

    @Column(name = "semestre_atual")
    private Integer semestreAtual;

    @Column(name = "carga_horaria")
    private Float cargaHoraria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @OneToMany(mappedBy = "discente")
    private List<Solicitacao> solicitacoes;

    @ManyToMany(mappedBy = "discentesGrupo")
    private List<Grupo> grupos;

    @ManyToMany(mappedBy = "discentesOp")
    private List<Oportunidade> oportunidades;
}
