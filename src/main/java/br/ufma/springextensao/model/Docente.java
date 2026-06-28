package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@Entity
@SuperBuilder
@Table(name = "docente")
@Inheritance // add strategy dps
@PrimaryKeyJoinColumn(name = "id_usuario")
@EqualsAndHashCode(callSuper = true)
public class Docente extends Usuario {
    @Column(name = "siape")
    private String siape;

    @Column(name = "departamento")
    private String departamento;

    @ManyToMany(mappedBy = "coordenadores")
    private List<Oportunidade> oportunidades;

    @OneToMany(mappedBy = "responsavel")
    private List<Grupo> grupos;

    public Docente() {
        super();
    }
}
