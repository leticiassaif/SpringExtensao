package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "solicitacao")
public class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String descricao;

    private int cargaHorario;

    // data inicio
    // data fim

    // status

    private String parecer;

    @ManyToOne
    @JoinColumn(name = "id_usuario") // discente?
    private Discente discente;
}
