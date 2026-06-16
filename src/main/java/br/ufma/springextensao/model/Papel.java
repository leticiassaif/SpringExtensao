package br.ufma.springextensao.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Papel {
    @Id
    private Integer id;
    private String papel; // nome do papel

    //TODO: revisar mapeamento do papel
}
