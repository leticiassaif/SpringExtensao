package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.PainelHorasDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepo usuarioRepo;

    @Mock
    CursoRepo cursoRepo;

    @Mock
    PapelRepo papelRepo;

    @Mock
    GrupoService grupoService;

    @InjectMocks
    UsuarioService usuarioService;

    // Helpers ----------------------------------------------------------

    private int nextPapelId = 1;

    private Papel papel(String nome) {
        Papel p = new Papel();
        p.setId(nextPapelId++);
        p.setNome(nome);
        return p;
    }

    private Discente discenteComCargo(Papel... cargos) {
        Discente d = Discente.builder()
                .nome("Joana")
                .email("joana@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>(List.of(cargos)))
                .matricula("2023001")
                .cargaHoraria(0)
                .grupos(new ArrayList<>())
                .build();
        d.setId(1);
        return d;
    }

    private Docente docenteComCargo(Papel... cargos) {
        Docente doc = Docente.builder()
                .nome("Carlos")
                .email("carlos@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>(List.of(cargos)))
                .build();
        doc.setId(2);
        return doc;
    }

    // cadastrarDiscente --------------------------------------------------

    @Nested
    class CadastrarDiscente {

        @Test
        void deveCadastrarDiscenteComCursoValido() {
            DiscenteDTO dto = new DiscenteDTO();
            dto.setNome("Maria");
            dto.setEmail("maria@ufma.br");
            dto.setSenha("senha123");
            dto.setMatricula("2024001");
            dto.setCargaHoraria(10);
            dto.setIdCurso(5);

            Curso curso = Curso.builder().id(5).nome("Computação").build();
            when(cursoRepo.findById(5)).thenReturn(Optional.of(curso));
            when(usuarioRepo.save(any(Discente.class))).thenAnswer(inv -> inv.getArgument(0));

            Discente resultado = usuarioService.cadastrarDiscente(dto);

            assertThat(resultado.getNome()).isEqualTo("Maria");
            assertThat(resultado.getEmail()).isEqualTo("maria@ufma.br");
            assertThat(resultado.isAtivo()).isTrue();
            assertThat(resultado.getCurso()).isEqualTo(curso);
            assertThat(resultado.getSenha()).isNotEqualTo("senha123");
            assertThat(new BCryptPasswordEncoder().matches("senha123", resultado.getSenha())).isTrue();

            verify(usuarioRepo).save(any(Discente.class));
        }

        @Test
        void deveLancarExcecaoQuandoCursoNaoExiste() {
            DiscenteDTO dto = new DiscenteDTO();
            dto.setIdCurso(999);
            dto.setSenha("senha123");

            when(cursoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso");

            verify(usuarioRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoIdCursoEhNulo() {
            DiscenteDTO dto = new DiscenteDTO();
            dto.setIdCurso(null);
            dto.setSenha("senha123");

            // mock retorna Optional.empty() para findById(null) por padrão
            when(cursoRepo.findById(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso");

            verify(usuarioRepo, never()).save(any());
        }
    }

    // cadastrarDocente ----------------------------------------------------

    @Nested
    class CadastrarDocente {

        @Test
        void deveCadastrarDocenteQuandoSolicitanteEhAdmin() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            DocenteDTO dto = new DocenteDTO();
            dto.setNome("Pedro");
            dto.setEmail("pedro@ufma.br");
            dto.setSenha("minhasenha");
            dto.setSiape("12345");
            dto.setDepartamento("DEINF");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(usuarioRepo.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

            Docente resultado = usuarioService.cadastrarDocente(solicitante, dto);

            assertThat(resultado.getNome()).isEqualTo("Pedro");
            assertThat(resultado.getSiape()).isEqualTo("12345");
            assertThat(resultado.getSenha()).isNotEqualTo("minhasenha");
            assertThat(new BCryptPasswordEncoder().matches("minhasenha", resultado.getSenha())).isTrue();
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhAdmin() {
            Papel docentePapel = papel("DOCENTE");
            Docente solicitante = docenteComCargo(docentePapel);
            Papel admin = papel("ADMIN");

            DocenteDTO dto = new DocenteDTO();
            dto.setSenha("senha");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.cadastrarDocente(solicitante, dto))
                    .isInstanceOf(SecurityException.class);

            verify(usuarioRepo, never()).save(any());
        }
    }

    // promoverDocente -------------------------------------------------------

    @Nested
    class PromoverDocente {

        @Test
        void devePromoverDocenteQuandoSolicitanteEhAdminECargoExiste() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Papel coordenador = papel("COORDENADOR");
            Docente alvo = docenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(2)).thenReturn(Optional.of(alvo));
            when(usuarioRepo.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

            Docente resultado = usuarioService.promoverDocente(solicitante, "coordenador", 2);

            assertThat(resultado.getCargos()).contains(coordenador);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhAdmin() {
            Papel docentePapel = papel("DOCENTE");
            Docente solicitante = docenteComCargo(docentePapel);
            Papel admin = papel("ADMIN");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", 2))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID");
        }

        @Test
        void deveLancarExcecaoQuandoCargoEhNulo() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, null, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo");
        }

        @Test
        void deveLancarExcecaoQuandoCargoNaoExiste() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("INEXISTENTE")).thenReturn(null);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "inexistente", 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Papel coordenador = papel("COORDENADOR");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioAlvoNaoEhDocente() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Papel coordenador = papel("COORDENADOR");
            Discente alvo = discenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(1)).thenReturn(Optional.of(alvo));

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é docente");
        }

        @Test
        void deveLancarExcecaoDeCargoInvalidoQuandoCargoEhEmBranco() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "   ", 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido");
        }

        // BUG CONHECIDO: promoverDocente() não verifica se o docente já possui o cargo;
        // getCargos().add(papel) cria duplicatas na lista. Fica VERMELHO até a correção.
        @Test
        void naoDeveDuplicarCargoSeDocenteJaPossui() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Docente solicitante = docenteComCargo(admin);
            Docente alvo = docenteComCargo(coordenador);
            alvo.setId(3);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(3)).thenReturn(Optional.of(alvo));
            when(usuarioRepo.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.promoverDocente(solicitante, "COORDENADOR", 3);

            assertThat(alvo.getCargos()).containsOnlyOnce(coordenador);
        }
    }
    // desativar -------------------------------------------------------------

    @Nested
    class Desativar {

        @Test
        void adminDeveDesativarQualquerUsuario() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Docente solicitante = docenteComCargo(admin);
            Discente alvo = discenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(1)).thenReturn(Optional.of(alvo));

            usuarioService.desativar(solicitante, 1);

            assertThat(alvo.isAtivo()).isFalse();
            verify(grupoService).removerDiscenteTodosGrupos(solicitante, 1);
            verify(usuarioRepo).save(alvo);
        }

        @Test
        void coordenadorDeveDesativarDiscente() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Docente solicitante = docenteComCargo(coordenador);
            Discente alvo = discenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(1)).thenReturn(Optional.of(alvo));

            usuarioService.desativar(solicitante, 1);

            assertThat(alvo.isAtivo()).isFalse();
            verify(grupoService).removerDiscenteTodosGrupos(solicitante, 1);
        }

        @Test
        void coordenadorNaoDeveDesativarDocente() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Docente solicitante = docenteComCargo(coordenador);
            Docente alvo = docenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(2)).thenReturn(Optional.of(alvo));

            assertThatThrownBy(() -> usuarioService.desativar(solicitante, 2))
                    .isInstanceOf(SecurityException.class);

            verify(usuarioRepo, never()).save(any());
            verify(grupoService, never()).removerDiscenteTodosGrupos(any(), any());
        }

        @Test
        void usuarioSemPermissaoNaoDeveDesativar() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Docente solicitante = docenteComCargo();
            Discente alvo = discenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(1)).thenReturn(Optional.of(alvo));

            assertThatThrownBy(() -> usuarioService.desativar(solicitante, 1))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Docente solicitante = docenteComCargo(papel("ADMIN"));

            when(usuarioRepo.findById(404)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.desativar(solicitante, 404))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void naoDeveRemoverDeGruposQuandoAlvoNaoEhDiscente() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Docente alvo = docenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(usuarioRepo.findById(2)).thenReturn(Optional.of(alvo));

            usuarioService.desativar(solicitante, 2);

            verify(grupoService, never()).removerDiscenteTodosGrupos(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            // buscarPorId(null) lança IAE "ID inválido." antes da verificação de permissão
            assertThatThrownBy(() -> usuarioService.desativar(docenteComCargo(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }
    }

    // anonimizar --------------------------------------------------------------

    @Nested
    class Anonimizar {

        @Test
        void adminDeveAnonimizarDiscente() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Discente alvo = discenteComCargo(papel("DISCENTE"));
            alvo.setId(7);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(usuarioRepo.findById(7)).thenReturn(Optional.of(alvo));

            usuarioService.anonimizar(solicitante, 7);

            assertThat(alvo.isAtivo()).isFalse();
            assertThat(alvo.getNome()).isEqualTo("Usuário Anonimizado");
            assertThat(alvo.getEmail()).isEqualTo("anonimo_7@sistema.local");
            assertThat(alvo.getSenha()).isEmpty();
            assertThat(alvo.getCargos()).isEmpty();
            verify(grupoService).removerDiscenteTodosGrupos(solicitante, 7);
            verify(usuarioRepo).save(alvo);
        }

        @Test
        void naoAdminNaoDeveAnonimizar() {
            Papel coordenador = papel("COORDENADOR");
            Docente solicitante = docenteComCargo(coordenador);
            Papel admin = papel("ADMIN");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.anonimizar(solicitante, 7))
                    .isInstanceOf(SecurityException.class);

            verify(usuarioRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(usuarioRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.anonimizar(solicitante, 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void naoDeveRemoverDeGruposQuandoAlvoAnonimizadoEhDocente() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Docente alvo = docenteComCargo();
            alvo.setId(5);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(usuarioRepo.findById(5)).thenReturn(Optional.of(alvo));

            usuarioService.anonimizar(solicitante, 5);

            assertThat(alvo.isAtivo()).isFalse();
            assertThat(alvo.getNome()).isEqualTo("Usuário Anonimizado");
            verify(grupoService, never()).removerDiscenteTodosGrupos(any(), any());
        }
    }

    // autenticar --------------------------------------------------------------

    @Nested
    class Autenticar {

        @Test
        void deveAutenticarComCredenciaisCorretas() {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            Discente usuario = discenteComCargo();
            usuario.setSenha(encoder.encode("senhaCorreta"));
            usuario.setEmail("joana@ufma.br");

            when(usuarioRepo.findByEmail("joana@ufma.br")).thenReturn(Optional.of(usuario));

            Usuario resultado = usuarioService.autenticar("joana@ufma.br", "senhaCorreta");

            assertThat(resultado).isEqualTo(usuario);
        }

        @Test
        void deveLancarExcecaoComEmailInvalido() {
            assertThatThrownBy(() -> usuarioService.autenticar("nao-eh-email", "qualquer"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email inválido");

            verifyNoInteractions(usuarioRepo);
        }

        @Test
        void deveLancarExcecaoComSenhaNula() {
            assertThatThrownBy(() -> usuarioService.autenticar("joana@ufma.br", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Senha inválida");
        }

        @Test
        void deveLancarExcecaoComSenhaEmBranco() {
            assertThatThrownBy(() -> usuarioService.autenticar("joana@ufma.br", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Senha inválida");
        }

        @Test
        void deveLancarExcecaoQuandoEmailNaoCadastrado() {
            when(usuarioRepo.findByEmail("ninguem@ufma.br")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.autenticar("ninguem@ufma.br", "qualquer123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nenhum usuário possui esse email");
        }

        @Test
        void deveLancarExcecaoComSenhaIncorreta() {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            Discente usuario = discenteComCargo();
            usuario.setSenha(encoder.encode("senhaCorreta"));
            usuario.setEmail("joana@ufma.br");

            when(usuarioRepo.findByEmail("joana@ufma.br")).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> usuarioService.autenticar("joana@ufma.br", "senhaErrada"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Senha incorreta");
        }
    }

    // buscarPorEmail / buscarPorId ----------------------------------------------

    @Nested
    class BuscarPorEmail {

        @Test
        void deveRetornarUsuarioQuandoEmailExiste() {
            Discente usuario = discenteComCargo();
            when(usuarioRepo.findByEmail("joana@ufma.br")).thenReturn(Optional.of(usuario));

            Usuario resultado = usuarioService.buscarPorEmail("joana@ufma.br");

            assertThat(resultado).isEqualTo(usuario);
        }

        @Test
        void deveRetornarNuloQuandoEmailNaoExiste() {
            when(usuarioRepo.findByEmail("ninguem@ufma.br")).thenReturn(Optional.empty());

            Usuario resultado = usuarioService.buscarPorEmail("ninguem@ufma.br");

            assertThat(resultado).isNull();
        }

        @Test
        void deveLancarExcecaoComEmailInvalido() {
            assertThatThrownBy(() -> usuarioService.buscarPorEmail("invalido"))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(usuarioRepo);
        }
    }

    @Nested
    class BuscarPorId {

        @Test
        void deveRetornarUsuarioQuandoIdExiste() {
            Discente usuario = discenteComCargo();
            when(usuarioRepo.findById(1)).thenReturn(Optional.of(usuario));

            Usuario resultado = usuarioService.buscarPorId(1);

            assertThat(resultado).isEqualTo(usuario);
        }

        @Test
        void deveRetornarNuloQuandoIdNaoExiste() {
            when(usuarioRepo.findById(404)).thenReturn(Optional.empty());

            Usuario resultado = usuarioService.buscarPorId(404);

            assertThat(resultado).isNull();
        }

        @Test
        void deveLancarExcecaoComIdNulo() {
            assertThatThrownBy(() -> usuarioService.buscarPorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");

            verifyNoInteractions(usuarioRepo);
        }
    }

    // hasPermissao --------------------------------------------------------------

    @Nested
    class HasPermissao {

        @Test
        void deveRetornarTrueQuandoUsuarioPossuiCargo() {
            Papel admin = papel("ADMIN");
            Discente usuario = discenteComCargo(admin);

            assertThat(UsuarioService.hasPermissao(usuario, admin)).isTrue();
        }

        @Test
        void deveRetornarFalseQuandoUsuarioNaoPossuiCargo() {
            Papel admin = papel("ADMIN");
            Discente usuario = discenteComCargo();

            assertThat(UsuarioService.hasPermissao(usuario, admin)).isFalse();
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioEhNulo() {
            Papel admin = papel("ADMIN");

            assertThatThrownBy(() -> UsuarioService.hasPermissao(null, admin))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário inválido");
        }
    }

    // painelHorasDTO -------------------------------------------------------------

    @Nested
    class PainelHoras {

        @Test
        void deveMontarPainelParaDiscente() {
            Curso curso = Curso.builder().id(1).cargaHoraria(200).build();
            Discente discente = discenteComCargo();
            discente.setCargaHoraria(80);
            discente.setCurso(curso);

            when(usuarioRepo.findById(1)).thenReturn(Optional.of(discente));

            PainelHorasDTO resultado = usuarioService.painelHorasDTO(1);

            assertThat(resultado.getCargaHorariaFeita()).isEqualTo(80);
            assertThat(resultado.getCargaHorariaTotal()).isEqualTo(200);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.painelHorasDTO(999))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docenteComCargo();
            when(usuarioRepo.findById(2)).thenReturn(Optional.of(docente));

            assertThatThrownBy(() -> usuarioService.painelHorasDTO(2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }
    }
}
