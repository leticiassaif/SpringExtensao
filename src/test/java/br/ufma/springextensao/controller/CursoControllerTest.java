package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.CursoDTO;
import br.ufma.springextensao.controller.dtos.UCEDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.service.CursoService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/*
 * NOTA: assume-se jakarta.servlet.http.HttpSession (Spring Boot 3). Se o projeto
 * usar Spring Boot 2 / javax, trocar o import para javax.servlet.http.HttpSession.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CursoControllerTest {

    @Mock
    CursoService cursoService;

    @Mock
    UsuarioService usuarioService;

    @Mock
    HttpSession session;

    @InjectMocks
    CursoController cursoController;

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
                .build();
        d.setId(nextId++);
        return d;
    }

    private Curso curso(String curriculo) {
        Curso c = Curso.builder()
                .nome("Ciência da Computação")
                .codigo("CC001")
                .curriculo(curriculo)
                .cargaHoraria(3200)
                .uces(new ArrayList<>())
                .discentes(new ArrayList<>())
                .build();
        c.setId(nextId++);
        return c;
    }

    private CursoDTO cursoDTO(String codigo, String curriculo, Integer cargaHoraria,
                              String dataInicio, String dataFim) {
        CursoDTO dto = new CursoDTO();
        dto.setCodigo(codigo);
        dto.setCurriculo(curriculo);
        dto.setCargaHoraria(cargaHoraria);
        dto.setDataInicio(dataInicio);
        dto.setDataFim(dataFim);
        return dto;
    }

    private UCEDTO uceDTO(String nome, Integer cargaHoraria, Integer idCurso) {
        UCEDTO dto = new UCEDTO();
        dto.setNome(nome);
        dto.setCargaHoraria(cargaHoraria);
        dto.setIdCurso(idCurso);
        return dto;
    }

    private UCE uce(String nome, Integer cargaHoraria, Curso curso) {
        return UCE.builder()
                .nome(nome)
                .cargaHoraria(cargaHoraria)
                .curso(curso)
                .build();
    }

    /** Simula um usuário autenticado na sessão. */
    private void logarComo(Usuario usuario) {
        when(session.getAttribute("IdUsuarioLogado")).thenReturn(usuario.getId());
        when(usuarioService.buscarPorId(usuario.getId())).thenReturn(usuario);
    }

    // cadastrarCurso (POST /api/curso/cadastrar) ----------------------------

    @Nested
    class CadastrarCurso {

        @Test
        void devePermitirCadastroQuandoDocenteEstaLogado() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "2020-01-01", "2025-01-01");
            Curso esperado = curso("PPC2020");
            when(cursoService.cadastrarCurso(solicitante, dto)).thenReturn(esperado);

            Curso resultado = cursoController.cadastrarCurso(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(cursoService, times(1)).cadastrarCurso(solicitante, dto);
        }

        @Test
        void devePermitirCadastroQuandoDiscenteEstaLogado() {
            // O controller não filtra por papel; quem decide permissão é o service.
            Discente solicitante = discente();
            logarComo(solicitante);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "2020-01-01", "2025-01-01");
            Curso esperado = curso("PPC2020");
            when(cursoService.cadastrarCurso(solicitante, dto)).thenReturn(esperado);

            Curso resultado = cursoController.cadastrarCurso(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(cursoService, times(1)).cadastrarCurso(solicitante, dto);
        }

        @Test
        void deveLancarSecurityExceptionQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);
            CursoDTO dto = cursoDTO("CC", "PPC", 3200, "2020-01-01", "2025-01-01");

            assertThatThrownBy(() -> cursoController.cadastrarCurso(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não está logado.");

            verify(cursoService, never()).cadastrarCurso(any(), any());
        }

        @Test
        void deveLancarSecurityExceptionQuandoSessaoNaoTemAtributo() {
            // session.getAttribute(...) retorna null por padrão (não foi stubado).
            when(usuarioService.buscarPorId(null)).thenReturn(null);
            CursoDTO dto = cursoDTO("CC", "PPC", 3200, "2020-01-01", "2025-01-01");

            assertThatThrownBy(() -> cursoController.cadastrarCurso(dto, session))
                    .isInstanceOf(SecurityException.class);

            verify(cursoService, never()).cadastrarCurso(any(), any());
        }

        // BUG CONHECIDO: cadastrarCurso() faz `(Integer) session.getAttribute(...)` sem
        // tratamento. Se o atributo de sessão estiver corrompido/com tipo inválido
        // (ex.: uma String em vez de Integer), o cast lança ClassCastException, que
        // vaza como erro 500 não controlado em vez de ser tratado como "não logado"
        // (SecurityException). Fica VERMELHO até o controller validar/capturar o cast.
        @Test
        void deveLancarExcecaoControladaQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");
            CursoDTO dto = cursoDTO("CC", "PPC", 3200, "2020-01-01", "2025-01-01");

            assertThatThrownBy(() -> cursoController.cadastrarCurso(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class);
        }

        @Test
        void devePropagarExcecaoDoServiceQuandoDtoEhInvalido() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            CursoDTO dto = cursoDTO(null, null, -100, "data-invalida", null);
            when(cursoService.cadastrarCurso(solicitante, dto))
                    .thenThrow(new IllegalArgumentException("Carga horária inválida"));

            assertThatThrownBy(() -> cursoController.cadastrarCurso(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária inválida");
        }

        @Test
        void devePermitirDtoNuloRepassandoParaOService() {
            // O controller não valida o DTO; a responsabilidade é do service.
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(cursoService.cadastrarCurso(solicitante, null))
                    .thenThrow(new IllegalArgumentException("DTO inválido"));

            assertThatThrownBy(() -> cursoController.cadastrarCurso(null, session))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(cursoService, times(1)).cadastrarCurso(solicitante, null);
        }
    }

    // buscarCurso (GET /api/curso/busca/{id}) --------------------------------

    @Nested
    class BuscarCurso {

        @Test
        void deveRetornarCursoQuandoExiste() {
            Curso esperado = curso("PPC2025");
            when(cursoService.buscaPorId(esperado.getId())).thenReturn(esperado);

            Curso resultado = cursoController.buscarCurso(esperado.getId());

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveRetornarNuloQuandoServiceNaoEncontraCurso() {
            when(cursoService.buscaPorId(404)).thenReturn(null);

            Curso resultado = cursoController.buscarCurso(404);

            assertThat(resultado).isNull();
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            when(cursoService.buscaPorId(null))
                    .thenThrow(new IllegalArgumentException("ID inválido"));

            assertThatThrownBy(() -> cursoController.buscarCurso(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }
    }

    // buscaVersao (GET /api/curso/busca/versao) ------------------------------

    @Nested
    class BuscaVersao {

        @Test
        void deveRetornarCursoQuandoVersaoExiste() {
            Curso esperado = curso("PPC2025");
            when(cursoService.buscarPorVersao("PPC2025")).thenReturn(esperado);

            Curso resultado = cursoController.buscaVersao("PPC2025");

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveRetornarNuloQuandoVersaoNaoExiste() {
            when(cursoService.buscarPorVersao("INEXISTENTE")).thenReturn(null);

            Curso resultado = cursoController.buscaVersao("INEXISTENTE");

            assertThat(resultado).isNull();
        }

        @Test
        void devePropagarExcecaoQuandoVersaoEhNula() {
            when(cursoService.buscarPorVersao(null))
                    .thenThrow(new IllegalArgumentException("Versão inválida"));

            assertThatThrownBy(() -> cursoController.buscaVersao(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Versão inválida");
        }

        @Test
        void devePropagarExcecaoQuandoVersaoEhEmBranco() {
            when(cursoService.buscarPorVersao("   "))
                    .thenThrow(new IllegalArgumentException("Versão inválida"));

            assertThatThrownBy(() -> cursoController.buscaVersao("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // buscaVigente (GET /api/curso/busca/vigente) ----------------------------

    @Nested
    class BuscaVigente {

        @Test
        void deveRetornarCursoVigente() {
            Curso vigente = curso("PPC2025");
            when(cursoService.buscarVigente()).thenReturn(vigente);

            Curso resultado = cursoController.buscaVigente();

            assertThat(resultado).isEqualTo(vigente);
        }

        @Test
        void deveRetornarNuloQuandoNaoHaVigente() {
            when(cursoService.buscarVigente()).thenReturn(null);

            Curso resultado = cursoController.buscaVigente();

            assertThat(resultado).isNull();
        }
    }

    // listaHistorico (GET /api/curso/historico) ------------------------------

    @Nested
    class ListaHistorico {

        @Test
        void deveRetornarTodosOsCursos() {
            Curso cursoA = curso("PPC2020");
            Curso cursoB = curso("PPC2025");
            when(cursoService.listaHistorico()).thenReturn(List.of(cursoA, cursoB));

            List<Curso> resultado = cursoController.listaHistorico();

            assertThat(resultado).containsExactly(cursoA, cursoB);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaCursos() {
            when(cursoService.listaHistorico()).thenReturn(List.of());

            List<Curso> resultado = cursoController.listaHistorico();

            assertThat(resultado).isEmpty();
        }
    }

    // cadastrarUCE (POST /api/curso/uce/cadastrar) ---------------------------

    @Nested
    class CadastrarUCE {

        @Test
        void devePermitirCadastroQuandoDocenteEstaLogado() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            Curso cursoPai = curso("PPC2025");
            UCEDTO dto = uceDTO("Cálculo I", 60, cursoPai.getId());
            UCE esperado = uce("Cálculo I", 60, cursoPai);
            when(cursoService.cadastrarUCE(solicitante, dto)).thenReturn(esperado);

            UCE resultado = cursoController.cadastrarUCE(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(cursoService, times(1)).cadastrarUCE(solicitante, dto);
        }

        @Test
        void devePermitirCadastroQuandoDiscenteEstaLogado() {
            Discente solicitante = discente();
            logarComo(solicitante);
            Curso cursoPai = curso("PPC2025");
            UCEDTO dto = uceDTO("Álgebra Linear", 60, cursoPai.getId());
            UCE esperado = uce("Álgebra Linear", 60, cursoPai);
            when(cursoService.cadastrarUCE(solicitante, dto)).thenReturn(esperado);

            UCE resultado = cursoController.cadastrarUCE(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(cursoService, times(1)).cadastrarUCE(solicitante, dto);
        }

        @Test
        void deveLancarSecurityExceptionQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(9);
            when(usuarioService.buscarPorId(9)).thenReturn(null);
            UCEDTO dto = uceDTO("Cálculo I", 60, 1);

            assertThatThrownBy(() -> cursoController.cadastrarUCE(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não está logado.");

            verify(cursoService, never()).cadastrarUCE(any(), any());
        }

        @Test
        void deveLancarSecurityExceptionQuandoSessaoNaoTemAtributo() {
            when(usuarioService.buscarPorId(null)).thenReturn(null);
            UCEDTO dto = uceDTO("Cálculo I", 60, 1);

            assertThatThrownBy(() -> cursoController.cadastrarUCE(dto, session))
                    .isInstanceOf(SecurityException.class);

            verify(cursoService, never()).cadastrarUCE(any(), any());
        }

        // BUG CONHECIDO: mesmo problema de cadastrarCurso() — cast não controlado de
        // session.getAttribute(...) para Integer. Fica VERMELHO até o controller
        // validar/capturar o cast e tratar como "não logado".
        @Test
        void deveLancarExcecaoControladaQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");
            UCEDTO dto = uceDTO("Cálculo I", 60, 1);

            assertThatThrownBy(() -> cursoController.cadastrarUCE(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class);
        }

        @Test
        void devePropagarExcecaoDoServiceQuandoCursoNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            UCEDTO dto = uceDTO("Cálculo I", 60, 999);
            when(cursoService.cadastrarUCE(solicitante, dto))
                    .thenThrow(new IllegalArgumentException("Curso não existe"));

            assertThatThrownBy(() -> cursoController.cadastrarUCE(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso não existe");
        }

        @Test
        void devePermitirDtoNuloRepassandoParaOService() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(cursoService.cadastrarUCE(solicitante, null))
                    .thenThrow(new IllegalArgumentException("DTO inválido"));

            assertThatThrownBy(() -> cursoController.cadastrarUCE(null, session))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(cursoService, times(1)).cadastrarUCE(solicitante, null);
        }
    }

    // buscaUCEporCurso (GET /api/curso/uce/busca/{id}) -----------------------

    @Nested
    class BuscaUCEPorCurso {

        @Test
        void deveRetornarUCEsDoCurso() {
            Curso cursoPai = curso("PPC2025");
            UCE uce1 = uce("Cálculo I", 60, cursoPai);
            UCE uce2 = uce("Álgebra Linear", 60, cursoPai);
            when(cursoService.buscaUCEPorPPC(cursoPai.getId())).thenReturn(List.of(uce1, uce2));

            List<UCE> resultado = cursoController.buscaUCEporCurso(cursoPai.getId());

            assertThat(resultado).containsExactly(uce1, uce2);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaUCEs() {
            when(cursoService.buscaUCEPorPPC(1)).thenReturn(List.of());

            List<UCE> resultado = cursoController.buscaUCEporCurso(1);

            assertThat(resultado).isEmpty();
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            when(cursoService.buscaUCEPorPPC(null))
                    .thenThrow(new IllegalArgumentException("ID inválido"));

            assertThatThrownBy(() -> cursoController.buscaUCEporCurso(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }

        @Test
        void devePropagarExcecaoQuandoCursoNaoExiste() {
            when(cursoService.buscaUCEPorPPC(999))
                    .thenThrow(new IllegalArgumentException("Curso não existe"));

            assertThatThrownBy(() -> cursoController.buscaUCEporCurso(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso não existe");
        }
    }
}
