package br.ufma.springextensao.model;

import br.ufma.springextensao.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inscricao")
public class Inscricao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "motivacao")
    private String motivacao;

    @Column(name = "status")
    private Enum<Status> status;

    @Column(name = "justificativa_cancelamento")
    private String justificativaCancelamento;

    @Column(name = "data_inscricao")
    private LocalDate dataInscricao;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Discente discente;

    @ManyToOne
    @JoinColumn(name = "id_oportunidade")
    private Oportunidade oportunidade;


}
