package br.ufma.springextensao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    protected Integer id;

    @Column(name = "nome")
    protected String nome;

    @Column(name = "email")
    protected String email;

    @Column(name = "senha")
    protected String senha;

    @Column(name = "ativo")
    protected boolean ativo;

    @ManyToMany
    @JoinTable(name = "usuario_papel",
                joinColumns = @JoinColumn(name = "id_usuario"),
                inverseJoinColumns = @JoinColumn(name = "id_papel"))
    List<Papel> cargos;
}
