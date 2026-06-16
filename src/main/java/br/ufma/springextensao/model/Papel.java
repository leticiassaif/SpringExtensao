package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "papel")
public class Papel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_papel")
    private Integer id;
    private String papel; // nome do papel

    @ManyToMany(mappedBy = "cargos")
    private List<Usuario> docentes;
}
