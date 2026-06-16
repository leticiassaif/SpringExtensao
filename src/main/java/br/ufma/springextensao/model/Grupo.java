package br.ufma.springextensao.model;

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

    private String descricao;

    @Column(name = "email")
    private String email;
    // private discente solicitante
    private String justificativaNegacao;
    // status -- decidir se vai usar enum

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    // docente(s) responsável(eis)
    @ManyToMany
    @JoinTable(name = "grupo_docente",
            joinColumns = @JoinColumn(name = "id_grupo"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private List<Usuario> docentes;

    @ManyToMany
    @JoinTable(name = "grupo_discente",
            joinColumns = @JoinColumn(name = "id_grupo"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private List<Usuario> discentesGrupo;
}
