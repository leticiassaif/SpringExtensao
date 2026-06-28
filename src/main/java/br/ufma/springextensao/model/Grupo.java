package br.ufma.springextensao.model;

import br.ufma.springextensao.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "grupo")
public class Grupo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo")
    private Integer id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "email")
    private String email;

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

//    // discentes que possuem cargos (diretor, vice-diretor, tesoureiro)
//    @ManyToMany
//    @JoinTable(name = "grupo_diretores",
//            joinColumns = @JoinColumn(name = "id_grupo"),
//            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
//    private List<Discente> diretores;
//
    @ManyToMany
    @JoinTable(name = "grupo_discente",
            joinColumns = @JoinColumn(name = "id_grupo"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    private List<Discente> membros; // ou usuário?

    @OneToMany(mappedBy = "grupo")
    private List<GrupoMembro> membrosHistorico;
}
