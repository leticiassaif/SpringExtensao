package br.ufma.springextensao.model;

import br.ufma.springextensao.enums.Status;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "grupo")
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "descricao") // *
    private String descricao;

    @Column(name = "email")
    private String email;

    // private discente solicitante

    @Transient // *
    private String justificativaNegacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    // docente responsável
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Docente responsavel;

    @ManyToMany
    @JoinTable(name = "grupo_discente",
            joinColumns = @JoinColumn(name = "id_grupo"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private List<Usuario> discentesGrupo;
}
