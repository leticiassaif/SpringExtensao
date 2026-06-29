package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "uce")
public class UCE {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_uce")
    private Integer id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "carga_horaria")
    private Integer cargaHoraria;

    @ManyToOne
    @JoinColumn(name = "id_curso")
    private Curso curso;
}
