package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    // data de fim da vigência do ppc
    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    // data de fim da vigência do ppc
    @Column(name = "data_fim")
    private LocalDate dataFim;

    @OneToMany(mappedBy = "curso")
    private List<Discente> discentes;

    @OneToMany(mappedBy = "curso")
    private List<Grupo> grupos;

    @OneToMany(mappedBy = "curso")
    private List<Oportunidade> oportunidades;
}
