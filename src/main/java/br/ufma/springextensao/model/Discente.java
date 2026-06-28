package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@Entity
@SuperBuilder
@Table(name = "discente")
@Inheritance // add strategy dps
@PrimaryKeyJoinColumn(name = "id_usuario")
@EqualsAndHashCode(callSuper = true)
public class Discente extends Usuario {
    @Column(name = "matricula")
    private String matricula;

    @Column(name = "semestre_atual")
    private Integer semestreAtual;

    @Column(name = "carga_horaria")
    private Integer cargaHoraria;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @OneToMany(mappedBy = "discente")
    private List<Solicitacao> solicitacoes;

//    // grupos em que o discente possui cargo
    @ManyToMany(mappedBy = "diretores")
    private List<Grupo> gruposDiretores;

//    @OneToMany(mappedBy = "diretor")
//    private List<Grupo> gruposDiretor;

    // grupos em que o discente apenas participa
    @ManyToMany(mappedBy = "discentesGrupo")
    private List<Grupo> grupos;

    @ManyToMany(mappedBy = "discentesOp")
    private List<Oportunidade> oportunidades;


    public Discente() {
        super();
    }
}
