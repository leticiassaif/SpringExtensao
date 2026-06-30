package br.ufma.springextensao.model;

import br.ufma.springextensao.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "solicitacao")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "carga_horaria")
    private Integer cargaHorario;

    @Column(name = "data_solicitacao")
    private LocalDate dataSolicitacao;

    @Column(name = "data_atual")
    private LocalDate dataAtual;

    @Column(name = "prazo_reenvio")
    private LocalDate prazoReenvio;

    @Column(name = "parecer")
    private String parecer;

    @Column(name = "status")
    private Status status;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Discente discente; // nao seria usuario?

    public LocalDate getPrazoAnalise() {
        return dataAtual.plusDays(10);
    }
}
