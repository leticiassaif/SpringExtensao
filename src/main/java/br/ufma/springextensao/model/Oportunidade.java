package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "oportunidade")
// @Builder
// construtor
public class Oportunidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_oportunidade")
    private Integer id;

    @Column(name = "titulo")
    private String titulo;

    private String descricao;
    // modalidade (enum)

    @Column(name = "carga_horaria")
    private Integer cargaHoraria;

    // @Column(name = "vagas") necessário?
    private int vagas;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    // status -- decidir se vai usar enum mesmo

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private Tipo tipo;

    // discentes que participam da oportunidade
    @ManyToMany
    @JoinTable(name = "oportunidade_discente",
            joinColumns = @JoinColumn(name = "id_oportunidade"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private List<Usuario> discentesOp;

    // docente(s) responsável(eis)
    @ManyToMany
    @JoinTable(name = "oportunidade_coordenador",
                joinColumns = @JoinColumn(name = "id_oportunidade"),
                inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private List<Usuario> coodernadores;
}
