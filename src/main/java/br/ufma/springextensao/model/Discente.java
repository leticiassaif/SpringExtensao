package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "discente")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Discente extends Usuario {
    @Column(name = "matricula")
    private String matricula;

    // ou Integer?
    @Column(name = "semestre_atual")
    private int semestreAtual;

    @Column(name = "carga_horaria")
    private int cargaHoraria;

    @OneToMany(mappedBy = "discente")
    private List<Solicitacao> solicitacoes;

    @ManyToMany(mappedBy = "")
    private List<Grupo> grupos;

    @ManyToOne
    @JoinColumn(name = "id_curso")
    private Curso curso;
}
