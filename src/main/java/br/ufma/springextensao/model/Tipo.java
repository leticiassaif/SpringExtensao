package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "tipo")
public class Tipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Integer id;

    @Column(name = "tipo")
    private String tipo; // nome do tipo

    @OneToMany(mappedBy = "tipo")
    private List<Oportunidade> oportunidades;
}
