package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@Entity
@SuperBuilder
@Table(name = "discente")
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

    @OneToMany(mappedBy = "discente")
    private List<Inscricao> inscricoes;

    // grupos em que o discente participa
    @ManyToMany(mappedBy = "membros")
    private List<Grupo> grupos;

    @OneToMany(mappedBy = "discente")
    private List<GrupoMembro> cargoHistorico;

    @ManyToMany(mappedBy = "discentesOp")
    private List<Oportunidade> oportunidades;


    public Discente() {
        super();
    }
}
