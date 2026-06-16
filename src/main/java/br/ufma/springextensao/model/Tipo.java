package br.ufma.springextensao.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Tipo {
    @Id
    private Integer id;
    private String tipo; // nome do tipo

    @OneToMany(mappedBy = "tipo")
    private List<Oportunidade> oportunidades;
}
