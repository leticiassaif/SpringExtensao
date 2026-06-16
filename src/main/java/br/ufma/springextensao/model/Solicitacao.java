package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "solicitacao")
public class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String descricao;
    private Integer cargaHorario;

    @Column(name = "data_solicitacao")
    private LocalDate dataSolicitacao;

    @Column(name = "data_atual")
    private LocalDate dataAtual;

    private String parecer;
    // status -- decidir se vai usar enum mesmo

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Discente discente; // nao seria usuario?
}
