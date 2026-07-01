package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.PainelHorasDTO;
import br.ufma.springextensao.controller.dtos.UsuarioDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioControllerTest {

    @Mock
    UsuarioService usuarioService;

    @Mock
    HttpSession session;

    @InjectMocks
    UsuarioController usuarioController;

    // Helpers ----------------------------------------------------------

    private int nextId = 1;

    private Papel papel(String nome) {
        Papel p = new Papel();
        p.setId(nextId++);
        p.setNome(nome);
        return p;
    }

    private Docente docente(Papel... cargos) {
        Docente d = Docente.builder()
                .nome("Carlos")
                .email("carlos@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>(List.of(cargos)))
                .build();
        d.setId(nextId++);
        return d;
    }

    private Discente discente() {
        Discente d = Discente.builder()
                .nome("Joana")
                .email("joana@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>())
                .matricula("2023001")
                .cargaHoraria(0)
                .solicitacoes(new ArrayList<>())
                .grupos(new ArrayList<>())
                .cargoHistorico(new ArrayList<>())
                .oportunidades(new ArrayList<>())
                .build();
        d.setId(nextId++);
        return d;
    }

    private UsuarioDTO loginDTO(String email, String senha) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }

    private DiscenteDTO discenteDTO(String nome, String email, String senha,
                                    String matricula, Integer cargaHoraria, Integer idCurso) {
        DiscenteDTO dto = new DiscenteDTO();
        dto.setNome(nome);
        dto.setEmail(email);
        dto.setSenha(senha);
        dto.setMatricula(matricula);
        dto.setCargaHoraria(cargaHoraria);
        dto.setIdCurso(idCurso);
        return dto;
    }

    private DocenteDTO docenteDTO(String nome, String email, String senha,
                                  String siape, String departamento) {
        DocenteDTO dto = new DocenteDTO();
        dto.setNome(nome);
        dto.setEmail(email);
        dto.setSenha(senha);
        dto.setSiape(siape);
        dto.setDepartamento(departamento);
        return dto;
    }

    /** Simula um usuário autenticado na sessão. */
    private void logarComo(Usuario usuario) {
        when(session.getAttribute("IdUsuarioLogado")).thenReturn(usuario.getId());
        when(usuarioService.buscarPorId(usuario.getId())).thenReturn(usuario);
    }

    // login (POST /api/usuario/login) ----------------------------------

    @Nested
    class Login {

        @Test
        void devePermitirLoginComCredenciaisValidas() {
            Usuario usuario = discente();
            UsuarioDTO dto = loginDTO("joana@ufma.br", "senha123");
            when(usuarioService.autenticar("joana@ufma.br", "senha123")).thenReturn(usuario);

            Usuario resultado = usuarioController.login(dto, session);

            assertThat(resultado).isEqualTo(usuario);
            verify(session, times(1)).setAttribute("IdUsuarioLogado", usuario.getId());
        }

        @Test
        void devePermitirLoginDeDocente() {
            Usuario usuario = docente(papel("ADMIN"));
            UsuarioDTO dto = loginDTO("carlos@ufma.br", "senha123");
            when(usuarioService.autenticar("carlos@ufma.br", "senha123")).thenReturn(usuario);

            Usuario resultado = usuarioController.login(dto, session);

            assertThat(resultado).isEqualTo(usuario);
            verify(session, times(1)).setAttribute("IdUsuarioLogado", usuario.getId());
        }

        @Test
        void deveLancarExcecaoQuandoEmailEhInvalido() {
            UsuarioDTO dto = loginDTO("email-invalido", "senha123");
            when(usuarioService.autenticar("email-invalido", "senha123"))
                    .thenThrow(new IllegalArgumentException("Email inválido."));

            assertThatThrownBy(() -> usuarioController.login(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email inválido.");

            verify(session, never()).setAttribute(anyString(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSenhaEhInvalida() {
            UsuarioDTO dto = loginDTO("joana@ufma.br", "");
            when(usuarioService.autenticar("joana@ufma.br", ""))
                    .thenThrow(new IllegalArgumentException("Senha inválida."));

            assertThatThrownBy(() -> usuarioController.login(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Senha inválida.");

            verify(session, never()).setAttribute(anyString(), any());
        }

        @Test
        void deveLancarExcecaoQuandoEmailNaoExiste() {
            UsuarioDTO dto = loginDTO("naoexiste@ufma.br", "senha123");
            when(usuarioService.autenticar("naoexiste@ufma.br", "senha123"))
                    .thenThrow(new IllegalArgumentException("Nenhum usuário possui esse email."));

            assertThatThrownBy(() -> usuarioController.login(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nenhum usuário possui esse email.");

            verify(session, never()).setAttribute(anyString(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSenhaEstaIncorreta() {
            UsuarioDTO dto = loginDTO("joana@ufma.br", "senhaErrada");
            when(usuarioService.autenticar("joana@ufma.br", "senhaErrada"))
                    .thenThrow(new SecurityException("Senha incorreta."));

            assertThatThrownBy(() -> usuarioController.login(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Senha incorreta.");

            verify(session, never()).setAttribute(anyString(), any());
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioEstaInativo() {
            UsuarioDTO dto = loginDTO("joana@ufma.br", "senha123");
            when(usuarioService.autenticar("joana@ufma.br", "senha123"))
                    .thenThrow(new IllegalStateException("O usuário precisa estar ativo."));

            assertThatThrownBy(() -> usuarioController.login(dto, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("O usuário precisa estar ativo.");

            verify(session, never()).setAttribute(anyString(), any());
        }

        @Test
        void devePropagarExcecaoQuandoEmailEhNulo() {
            UsuarioDTO dto = loginDTO(null, "senha123");
            when(usuarioService.autenticar(null, "senha123"))
                    .thenThrow(new IllegalArgumentException("Email inválido."));

            assertThatThrownBy(() -> usuarioController.login(dto, session))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void devePropagarExcecaoQuandoSenhaEhNula() {
            UsuarioDTO dto = loginDTO("joana@ufma.br", null);
            when(usuarioService.autenticar("joana@ufma.br", null))
                    .thenThrow(new IllegalArgumentException("Senha inválida."));

            assertThatThrownBy(() -> usuarioController.login(dto, session))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // logout (POST /api/usuario/logout) ---------------------------------

    @Nested
    class Logout {

        @Test
        void deveInvalidarSessao() {
            usuarioController.logout(session);

            verify(session, times(1)).invalidate();
        }
    }

    // cadastrarDiscente (POST /api/usuario/cadastrar/discente) ----------

    @Nested
    class CadastrarDiscente {

        @Test
        void devePermitirCadastroSemNecessidadeDeLogin() {
            // O controller não exige sessão para autocadastro de discente;
            // a validação de curso é responsabilidade do service.
            DiscenteDTO dto = discenteDTO("Joana", "joana@ufma.br", "senha123", "2023001", 0, 1);
            Discente esperado = discente();
            when(usuarioService.cadastrarDiscente(dto)).thenReturn(esperado);

            Discente resultado = usuarioController.cadastrarDiscente(dto);

            assertThat(resultado).isEqualTo(esperado);
            verify(usuarioService, times(1)).cadastrarDiscente(dto);
        }

        @Test
        void deveLancarExcecaoQuandoCursoNaoExiste() {
            DiscenteDTO dto = discenteDTO("Joana", "joana@ufma.br", "senha123", "2023001", 0, 999);
            when(usuarioService.cadastrarDiscente(dto))
                    .thenThrow(new IllegalArgumentException("Curso com esse ID não existe."));

            assertThatThrownBy(() -> usuarioController.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso com esse ID não existe.");
        }

        @Test
        void devePermitirDtoNuloRepassandoParaOService() {
            when(usuarioService.cadastrarDiscente(null))
                    .thenThrow(new IllegalArgumentException("DTO inválido"));

            assertThatThrownBy(() -> usuarioController.cadastrarDiscente(null))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(usuarioService, times(1)).cadastrarDiscente(null);
        }

        @Test
        void devePropagarExcecaoComIdCursoNulo() {
            DiscenteDTO dto = discenteDTO("Joana", "joana@ufma.br", "senha123", "2023001", 0, null);
            when(usuarioService.cadastrarDiscente(dto))
                    .thenThrow(new IllegalArgumentException("Curso com esse ID não existe."));

            assertThatThrownBy(() -> usuarioController.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void devePropagarExcecaoComCargaHorariaNegativa() {
            DiscenteDTO dto = discenteDTO("Joana", "joana@ufma.br", "senha123", "2023001", -10, 1);
            when(usuarioService.cadastrarDiscente(dto))
                    .thenThrow(new IllegalArgumentException("Carga horária inválida."));

            assertThatThrownBy(() -> usuarioController.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoEmailEhInvalido() {
            DiscenteDTO dto = discenteDTO("Joana", "email-invalido", "senha123", "2023001", 0, 1);
            when(usuarioService.cadastrarDiscente(dto))
                    .thenThrow(new IllegalArgumentException("Formatação de email incorreta."));

            assertThatThrownBy(() -> usuarioController.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Formatação de email incorreta.");
        }

        @Test
        void deveLancarExcecaoQuandoSenhaEhEmBranco() {
            DiscenteDTO dto = discenteDTO("Joana", "joana@ufma.br", "   ", "2023001", 0, 1);
            when(usuarioService.cadastrarDiscente(dto))
                    .thenThrow(new IllegalArgumentException("Senha não pode ser vazia."));

            assertThatThrownBy(() -> usuarioController.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Senha não pode ser vazia.");
        }
    }

    // cadastrarDocente (POST /api/usuario/cadastrar/docente) ------------

    @Nested
    class CadastrarDocente {

        @Test
        void devePermitirCadastroQuandoSolicitanteEhAdmin() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            DocenteDTO dto = docenteDTO("Carlos", "carlos@ufma.br", "senha123", "1234567", "DEINF");
            Docente esperado = docente();
            when(usuarioService.cadastrarDocente(solicitante, dto)).thenReturn(esperado);

            Docente resultado = usuarioController.cadastrarDocente(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(usuarioService, times(1)).cadastrarDocente(solicitante, dto);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhAdmin() {
            Docente solicitante = docente();
            logarComo(solicitante);
            DocenteDTO dto = docenteDTO("Carlos", "carlos@ufma.br", "senha123", "1234567", "DEINF");
            when(usuarioService.cadastrarDocente(solicitante, dto))
                    .thenThrow(new SecurityException("O solicitante não possui permissão para cadastrar um docente."));

            assertThatThrownBy(() -> usuarioController.cadastrarDocente(dto, session))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);
            DocenteDTO dto = docenteDTO("Carlos", "carlos@ufma.br", "senha123", "1234567", "DEINF");

            assertThatThrownBy(() -> usuarioController.cadastrarDocente(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(usuarioService, never()).cadastrarDocente(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            DocenteDTO dto = docenteDTO("Carlos", "carlos@ufma.br", "senha123", "1234567", "DEINF");

            assertThatThrownBy(() -> usuarioController.cadastrarDocente(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(usuarioService, never()).cadastrarDocente(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");
            DocenteDTO dto = docenteDTO("Carlos", "carlos@ufma.br", "senha123", "1234567", "DEINF");

            assertThatThrownBy(() -> usuarioController.cadastrarDocente(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePermitirDtoNuloRepassandoParaOService() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.cadastrarDocente(solicitante, null))
                    .thenThrow(new IllegalArgumentException("DTO inválido"));

            assertThatThrownBy(() -> usuarioController.cadastrarDocente(null, session))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(usuarioService, times(1)).cadastrarDocente(solicitante, null);
        }

        @Test
        void deveLancarExcecaoQuandoEmailEhInvalido() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            DocenteDTO dto = docenteDTO("Carlos", "email-invalido", "senha123", "1234567", "DEINF");
            when(usuarioService.cadastrarDocente(solicitante, dto))
                    .thenThrow(new IllegalArgumentException("Formatação de email incorreta."));

            assertThatThrownBy(() -> usuarioController.cadastrarDocente(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Formatação de email incorreta.");
        }

        @Test
        void deveLancarExcecaoQuandoSenhaEhEmBranco() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            DocenteDTO dto = docenteDTO("Carlos", "carlos@ufma.br", "   ", "1234567", "DEINF");
            when(usuarioService.cadastrarDocente(solicitante, dto))
                    .thenThrow(new IllegalArgumentException("Senha não pode ser vazia."));

            assertThatThrownBy(() -> usuarioController.cadastrarDocente(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Senha não pode ser vazia.");
        }
    }

    // promoverDocente (PATCH /api/usuario/promover/docente/{id}) --------

    @Nested
    class PromoverDocente {

        @Test
        void devePermitirPromocaoQuandoSolicitanteEhAdmin() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            Docente alvo = docente();
            when(usuarioService.promoverDocente(solicitante, "COORDENADOR", alvo.getId())).thenReturn(alvo);

            Docente resultado = usuarioController.promoverDocente(alvo.getId(), "COORDENADOR", session);

            assertThat(resultado).isEqualTo(alvo);
            verify(usuarioService, times(1)).promoverDocente(solicitante, "COORDENADOR", alvo.getId());
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(usuarioService.promoverDocente(solicitante, "COORDENADOR", 5))
                    .thenThrow(new SecurityException("O usuário não possui permissão para promover um docente."));

            assertThatThrownBy(() -> usuarioController.promoverDocente(5, "COORDENADOR", session))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.promoverDocente(solicitante, "COORDENADOR", null))
                    .thenThrow(new IllegalArgumentException("ID inválido."));

            assertThatThrownBy(() -> usuarioController.promoverDocente(null, "COORDENADOR", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido.");
        }

        @Test
        void deveLancarExcecaoQuandoCargoEhEmBranco() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.promoverDocente(solicitante, "   ", 5))
                    .thenThrow(new IllegalArgumentException("Cargo inválido."));

            assertThatThrownBy(() -> usuarioController.promoverDocente(5, "   ", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido.");
        }

        @Test
        void deveLancarExcecaoQuandoCargoEhNulo() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.promoverDocente(solicitante, null, 5))
                    .thenThrow(new IllegalArgumentException("Cargo inválido."));

            assertThatThrownBy(() -> usuarioController.promoverDocente(5, null, session))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoCargoNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.promoverDocente(solicitante, "INEXISTENTE", 5))
                    .thenThrow(new IllegalArgumentException("Cargo não existe."));

            assertThatThrownBy(() -> usuarioController.promoverDocente(5, "INEXISTENTE", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo não existe.");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioAlvoNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.promoverDocente(solicitante, "COORDENADOR", 999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> usuarioController.promoverDocente(999, "COORDENADOR", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioAlvoNaoEhDocente() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.promoverDocente(solicitante, "COORDENADOR", 5))
                    .thenThrow(new IllegalArgumentException("Usuário não é docente."));

            assertThatThrownBy(() -> usuarioController.promoverDocente(5, "COORDENADOR", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é docente.");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> usuarioController.promoverDocente(5, "COORDENADOR", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(usuarioService, never()).promoverDocente(any(), anyString(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> usuarioController.promoverDocente(5, "COORDENADOR", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(usuarioService, never()).promoverDocente(any(), anyString(), any());
        }
    }

    // desativar (PATCH /api/usuario/desativar/{id}) ----------------------

    @Nested
    class Desativar {

        @Test
        void devePermitirDesativacaoPorAdmin() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            Discente alvo = discente();
            alvo.setAtivo(false);
            when(usuarioService.desativar(solicitante, alvo.getId())).thenReturn(alvo);

            Usuario resultado = usuarioController.desativar(alvo.getId(), session);

            assertThat(resultado).isEqualTo(alvo);
            verify(usuarioService, times(1)).desativar(solicitante, alvo.getId());
        }

        @Test
        void devePermitirCoordenadorDesativarDiscente() {
            Docente solicitante = docente(papel("COORDENADOR"));
            logarComo(solicitante);
            Discente alvo = discente();
            alvo.setAtivo(false);
            when(usuarioService.desativar(solicitante, alvo.getId())).thenReturn(alvo);

            Usuario resultado = usuarioController.desativar(alvo.getId(), session);

            assertThat(resultado).isEqualTo(alvo);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(usuarioService.desativar(solicitante, 5))
                    .thenThrow(new SecurityException("O solicitante não possui permissão para desativar o usuário"));

            assertThatThrownBy(() -> usuarioController.desativar(5, session))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioAlvoNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.desativar(solicitante, 999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe"));

            assertThatThrownBy(() -> usuarioController.desativar(999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> usuarioController.desativar(5, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(usuarioService, never()).desativar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> usuarioController.desativar(5, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(usuarioService, never()).desativar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> usuarioController.desativar(5, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.desativar(solicitante, null))
                    .thenThrow(new IllegalArgumentException("Usuário não existe"));

            assertThatThrownBy(() -> usuarioController.desativar(null, session))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // anonimizar (PATCH /api/usuario/anonimizar/{id}) --------------------

    @Nested
    class Anonimizar {

        @Test
        void devePermitirAnonimizacaoPorAdmin() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            Discente alvo = discente();
            when(usuarioService.anonimizar(solicitante, alvo.getId())).thenReturn(alvo);

            Usuario resultado = usuarioController.anonimizar(alvo.getId(), session);

            assertThat(resultado).isEqualTo(alvo);
            verify(usuarioService, times(1)).anonimizar(solicitante, alvo.getId());
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhAdmin() {
            Docente solicitante = docente(papel("COORDENADOR"));
            logarComo(solicitante);
            when(usuarioService.anonimizar(solicitante, 5))
                    .thenThrow(new SecurityException("O solicitante não possui permissão para anonimizar o usuário"));

            assertThatThrownBy(() -> usuarioController.anonimizar(5, session))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioAlvoNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(usuarioService.anonimizar(solicitante, 999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe"));

            assertThatThrownBy(() -> usuarioController.anonimizar(999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> usuarioController.anonimizar(5, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(usuarioService, never()).anonimizar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> usuarioController.anonimizar(5, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(usuarioService, never()).anonimizar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> usuarioController.anonimizar(5, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }
    }

    // buscaEmail (GET /api/usuario/email/{email}) -------------------------

    @Nested
    class BuscaEmail {

        @Test
        void deveRetornarUsuarioQuandoEmailExiste() {
            Usuario esperado = discente();
            when(usuarioService.buscarPorEmail("joana@ufma.br")).thenReturn(esperado);

            Usuario resultado = usuarioController.buscaEmail("joana@ufma.br");

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveRetornarNuloQuandoEmailNaoEncontrado() {
            when(usuarioService.buscarPorEmail("inexistente@ufma.br")).thenReturn(null);

            Usuario resultado = usuarioController.buscaEmail("inexistente@ufma.br");

            assertThat(resultado).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoEmailEhInvalido() {
            when(usuarioService.buscarPorEmail("email-invalido"))
                    .thenThrow(new IllegalArgumentException("Email inválido."));

            assertThatThrownBy(() -> usuarioController.buscaEmail("email-invalido"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email inválido.");
        }

        @Test
        void devePropagarExcecaoQuandoEmailEhVazio() {
            when(usuarioService.buscarPorEmail(""))
                    .thenThrow(new IllegalArgumentException("Email inválido."));

            assertThatThrownBy(() -> usuarioController.buscaEmail(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void devePropagarExcecaoQuandoEmailEhNulo() {
            when(usuarioService.buscarPorEmail(null))
                    .thenThrow(new IllegalArgumentException("Email inválido."));

            assertThatThrownBy(() -> usuarioController.buscaEmail(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // bucaId (GET /api/usuario/id/{id}) ------------------------------------

    @Nested
    class BuscaId {

        @Test
        void deveRetornarUsuarioQuandoIdExiste() {
            Usuario esperado = discente();
            when(usuarioService.buscarPorId(esperado.getId())).thenReturn(esperado);

            Usuario resultado = usuarioController.buscaId(esperado.getId());

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveRetornarNuloQuandoIdNaoEncontrado() {
            when(usuarioService.buscarPorId(404)).thenReturn(null);

            Usuario resultado = usuarioController.buscaId(404);

            assertThat(resultado).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            when(usuarioService.buscarPorId(null))
                    .thenThrow(new IllegalArgumentException("ID inválido."));

            assertThatThrownBy(() -> usuarioController.buscaId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido.");
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNegativo() {
            when(usuarioService.buscarPorId(-1)).thenReturn(null);

            Usuario resultado = usuarioController.buscaId(-1);

            assertThat(resultado).isNull();
        }

        @Test
        void devePropagarExcecaoQuandoIdEhZero() {
            when(usuarioService.buscarPorId(0)).thenReturn(null);

            Usuario resultado = usuarioController.buscaId(0);

            assertThat(resultado).isNull();
        }
    }

    // painelHorasDTO (GET /api/usuario/painel/{id}) ------------------------

    @Nested
    class PainelHoras {

        @Test
        void deveRetornarPainelQuandoDiscenteExiste() {
            PainelHorasDTO esperado = PainelHorasDTO.builder()
                    .cargaHorariaFeita(100)
                    .cargaHorariaTotal(3200)
                    .build();
            when(usuarioService.painelHorasDTO(1)).thenReturn(esperado);

            PainelHorasDTO resultado = usuarioController.painelHorasDTO(1);

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.painelHorasDTO(999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe"));

            assertThatThrownBy(() -> usuarioController.painelHorasDTO(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            when(usuarioService.painelHorasDTO(5))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> usuarioController.painelHorasDTO(5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é discente.");
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            when(usuarioService.painelHorasDTO(null))
                    .thenThrow(new IllegalArgumentException("ID inválido."));

            assertThatThrownBy(() -> usuarioController.painelHorasDTO(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}