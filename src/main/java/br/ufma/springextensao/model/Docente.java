package br.ufma.springextensao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "docente")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Docente extends Usuario {
    @Column(name = "siape")
    private String siape;

    @Column(name = "departamento")
    private String departamento;
}
