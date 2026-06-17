package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "docente")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Docente extends Usuario {
    @Column(name = "siape")
    private String siape;

    @Column(name = "departamento")
    private String departamento;

    @ManyToMany(mappedBy = "coordenadores")
    private List<Oportunidade> oportunidades;

    @ManyToMany(mappedBy = "docentes")
    private List<Grupo> grupos;
}
