package br.ufma.springextensao.model;

import br.ufma.springextensao.enums.StatusOp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "oportunidade")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Oportunidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_oportunidade")
    private Integer id;

    @Column(name = "titulo")
    private String titulo;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "carga_horaria")
    private Integer cargaHoraria;

    @Column(name = "vagas")
    private Integer vagas;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "status")
    private Enum <StatusOp> status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @ManyToOne//(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo")
    private Tipo tipo;

    @OneToMany(mappedBy = "oportunidade")
    private List<Inscricao> inscricoes;

    // discentes que participam da oportunidade
    @ManyToMany
    @JoinTable(name = "oportunidade_discente",
            joinColumns = @JoinColumn(name = "id_oportunidade"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private List<Discente> discentesOp;

    // docente(s) responsável(eis)
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario coordenador;
}
