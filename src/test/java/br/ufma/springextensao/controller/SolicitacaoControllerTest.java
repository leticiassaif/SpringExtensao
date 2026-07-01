package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.SolicitacaoRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import br.ufma.springextensao.service.SolicitacaoService;
import br.ufma.springextensao.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Duas famílias de testes: (1) Controller, mockando SolicitacaoService — sessão/permissão
// e repasse de exceções; (2) regras de negócio, instanciando o SolicitacaoService real com
// repositórios mockados via ReflectionTestUtils, já que o controller não valida nada sozinho.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitacaoControllerTest {

    // ---- Mocks para os testes de CONTROLLER --------------------------------
    @Mock
    SolicitacaoService solicitacaoService;

    @Mock
    UsuarioService usuarioService;

    @Mock
    HttpSession session;

    @InjectMocks
    SolicitacaoController solicitacaoController;

    // ---- Mocks para instanciar o SERVICE real (regras de negócio) ---------
    @Mock
    SolicitacaoRepo solicitacaoRepo;

    @Mock
    PapelRepo papelRepo;

    @Mock
    UsuarioRepo usuarioRepo;

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

    private Discente discente(Integer cargaHorariaAtual, Papel... cargos) {
        Discente d = Discente.builder()
                .nome("Joana")
                .email("joana@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargaHoraria(cargaHorariaAtual)
                .cargos(new ArrayList<>(List.of(cargos)))
                .build();
        d.setId(nextId++);
        return d;
    }

    private Discente discente() {
        return discente(0);
    }

    private SolicitacaoDTO solicitacaoDTO(String descricao, Integer cargaHoraria,
                                          String dataSolicitacao, Integer idDiscente) {
        return SolicitacaoDTO.builder()
                .descricao(descricao)
                .cargaHoraria(cargaHoraria)
                .dataSolicitacao(dataSolicitacao)
                .idDiscente(idDiscente)
                .build();
    }

    private Solicitacao solicitacaoEntity(Discente discente, Status status, Integer cargaHorario,
                                          LocalDate dataAtual, LocalDate prazoReenvio, String parecer) {
        Solicitacao s = Solicitacao.builder()
                .descricao("Participação em evento acadêmico")
                .discente(discente)
                .cargaHorario(cargaHorario)
                .dataSolicitacao(LocalDate.now())
                .dataAtual(dataAtual)
                .prazoReenvio(prazoReenvio)
                .parecer(parecer)
                .status(status)
                .build();
        s.setId(nextId++);
        return s;
    }

    /** Simula um usuário autenticado na sessão (mesmo padrão de CursoControllerTest). */
    private void logarComo(Usuario usuario) {
        when(session.getAttribute("IdUsuarioLogado")).thenReturn(usuario.getId());
        when(usuarioService.buscarPorId(usuario.getId())).thenReturn(usuario);
    }

    /** Instancia o SolicitacaoService real, injetando os mocks de repositório/colaboradores. */
    private SolicitacaoService novoServiceReal() {
        SolicitacaoService s = new SolicitacaoService();
        ReflectionTestUtils.setField(s, "solicitacaoRepo", solicitacaoRepo);
        ReflectionTestUtils.setField(s, "usuarioService", usuarioService);
        ReflectionTestUtils.setField(s, "papelRepo", papelRepo);
        ReflectionTestUtils.setField(s, "usuarioRepo", usuarioRepo);
        return s;
    }

    // =========================================================================
    // ===================  1) TESTES DE CONTROLLER (mockado)  ===============
    // =========================================================================

    // submeter (POST /api/solicitacao/submeter) ------------------------------

    @Nested
    class Submeter {

        @Test
        void devePermitirSubmissaoQuandoDadosValidos() {
            SolicitacaoDTO dto = solicitacaoDTO("Participação em evento", 20, "2024-05-10", 1);
            Solicitacao esperado = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoService.submeter(dto)).thenReturn(esperado);

            Solicitacao resultado = solicitacaoController.submeter(dto);

            assertThat(resultado).isEqualTo(esperado);
            verify(solicitacaoService, times(1)).submeter(dto);
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "2024-05-10", 999);
            when(solicitacaoService.submeter(dto))
                    .thenThrow(new IllegalArgumentException("Usuário não existe"));

            assertThatThrownBy(() -> solicitacaoController.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "2024-05-10", 1);
            when(solicitacaoService.submeter(dto))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> solicitacaoController.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void devePropagarExcecaoQuandoDataDeSolicitacaoEhNula() {
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, null, 1);
            when(solicitacaoService.submeter(dto))
                    .thenThrow(new IllegalArgumentException("Data de solicitação inválida."));

            assertThatThrownBy(() -> solicitacaoController.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de solicitação inválida");
        }

        @Test
        void devePropagarExcecaoQuandoDataDeSolicitacaoEhEmBranco() {
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "   ", 1);
            when(solicitacaoService.submeter(dto))
                    .thenThrow(new IllegalArgumentException("Data de solicitação inválida."));

            assertThatThrownBy(() -> solicitacaoController.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de solicitação inválida");
        }

        @Test
        void devePropagarExcecaoQuandoDataDeSolicitacaoEhInvalida() {
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "31/12/2024", 1);
            when(solicitacaoService.submeter(dto))
                    .thenThrow(new IllegalArgumentException("Data de solicitação inválida."));

            assertThatThrownBy(() -> solicitacaoController.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de solicitação inválida");
        }

        @Test
        void devePermitirDtoNuloRepassandoParaOService() {
            // O controller não valida o DTO; a responsabilidade é do service.
            when(solicitacaoService.submeter(null))
                    .thenThrow(new IllegalArgumentException("DTO inválido"));

            assertThatThrownBy(() -> solicitacaoController.submeter(null))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(solicitacaoService, times(1)).submeter(null);
        }
    }

    // aprovar (PATCH /api/solicitacao/aprovar/{id}) ---------------------------

    @Nested
    class Aprovar {

        @Test
        void devePermitirAprovacaoQuandoAdminEstaLogado() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            Solicitacao esperado = solicitacaoEntity(discente(), Status.APROVADO, 20, LocalDate.now(), null, null);
            when(solicitacaoService.aprovar(solicitante, esperado.getId())).thenReturn(esperado);

            Solicitacao resultado = solicitacaoController.aprovar(esperado.getId(), session);

            assertThat(resultado).isEqualTo(esperado);
            verify(solicitacaoService, times(1)).aprovar(solicitante, esperado.getId());
        }

        @Test
        void devePermitirAprovacaoQuandoCoordenadorEstaLogado() {
            Docente solicitante = docente(papel("COORDENADOR"));
            logarComo(solicitante);
            Solicitacao esperado = solicitacaoEntity(discente(), Status.APROVADO, 20, LocalDate.now(), null, null);
            when(solicitacaoService.aprovar(solicitante, esperado.getId())).thenReturn(esperado);

            Solicitacao resultado = solicitacaoController.aprovar(esperado.getId(), session);

            assertThat(resultado).isEqualTo(esperado);
            verify(solicitacaoService, times(1)).aprovar(solicitante, esperado.getId());
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> solicitacaoController.aprovar(1, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(solicitacaoService, never()).aprovar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> solicitacaoController.aprovar(1, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(solicitacaoService, never()).aprovar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> solicitacaoController.aprovar(1, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoDoServiceQuandoSemPermissao() {
            Discente solicitante = discente();
            logarComo(solicitante);
            when(solicitacaoService.aprovar(solicitante, 1))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> solicitacaoController.aprovar(1, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");
        }

        @Test
        void devePropagarExcecaoDoServiceQuandoSolicitacaoNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.aprovar(solicitante, 999))
                    .thenThrow(new IllegalArgumentException("Solicitação não existe."));

            assertThatThrownBy(() -> solicitacaoController.aprovar(999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void devePropagarExcecaoDoServiceQuandoSolicitacaoNaoEstaPendente() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.aprovar(solicitante, 1))
                    .thenThrow(new IllegalStateException("Solicitação não está pendente"));

            assertThatThrownBy(() -> solicitacaoController.aprovar(1, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }

        @Test
        void devePropagarExcecaoDoServiceQuandoCargaHorariaInvalida() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.aprovar(solicitante, 1))
                    .thenThrow(new IllegalArgumentException("Carga horária da solicitação inválida."));

            assertThatThrownBy(() -> solicitacaoController.aprovar(1, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária");
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.aprovar(solicitante, null))
                    .thenThrow(new IllegalArgumentException("ID inválido."));

            assertThatThrownBy(() -> solicitacaoController.aprovar(null, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }
    }

    // indeferir (PATCH /api/solicitacao/indeferir/{id}) -----------------------

    @Nested
    class Indeferir {

        @Test
        void devePermitirIndeferimentoQuandoAdminEstaLogado() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            Solicitacao esperado = solicitacaoEntity(discente(), Status.INDEFERIDO, 20,
                    LocalDate.now(), LocalDate.now().plusDays(5), "Documentação insuficiente");
            when(solicitacaoService.indeferir(solicitante, esperado.getId(), "Documentação insuficiente"))
                    .thenReturn(esperado);

            Solicitacao resultado = solicitacaoController.indeferir(esperado.getId(), "Documentação insuficiente", session);

            assertThat(resultado).isEqualTo(esperado);
            verify(solicitacaoService, times(1))
                    .indeferir(solicitante, esperado.getId(), "Documentação insuficiente");
        }

        @Test
        void devePermitirIndeferimentoQuandoCoordenadorEstaLogado() {
            Docente solicitante = docente(papel("COORDENADOR"));
            logarComo(solicitante);
            Solicitacao esperado = solicitacaoEntity(discente(), Status.INDEFERIDO, 20,
                    LocalDate.now(), LocalDate.now().plusDays(5), "Fora do prazo");
            when(solicitacaoService.indeferir(solicitante, esperado.getId(), "Fora do prazo"))
                    .thenReturn(esperado);

            Solicitacao resultado = solicitacaoController.indeferir(esperado.getId(), "Fora do prazo", session);

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> solicitacaoController.indeferir(1, "parecer", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(solicitacaoService, never()).indeferir(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> solicitacaoController.indeferir(1, "parecer", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(solicitacaoService, never()).indeferir(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> solicitacaoController.indeferir(1, "parecer", session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoParecerEhNulo() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.indeferir(solicitante, 1, null))
                    .thenThrow(new IllegalArgumentException("Parecer inválido."));

            assertThatThrownBy(() -> solicitacaoController.indeferir(1, null, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parecer inválido");
        }

        @Test
        void devePropagarExcecaoQuandoParecerEhEmBranco() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.indeferir(solicitante, 1, "   "))
                    .thenThrow(new IllegalArgumentException("Parecer inválido."));

            assertThatThrownBy(() -> solicitacaoController.indeferir(1, "   ", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parecer inválido");
        }

        @Test
        void devePropagarExcecaoQuandoSemPermissao() {
            Discente solicitante = discente();
            logarComo(solicitante);
            when(solicitacaoService.indeferir(solicitante, 1, "parecer"))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> solicitacaoController.indeferir(1, "parecer", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitacaoNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.indeferir(solicitante, 999, "parecer"))
                    .thenThrow(new IllegalArgumentException("Solicitação não existe."));

            assertThatThrownBy(() -> solicitacaoController.indeferir(999, "parecer", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitacaoNaoEstaPendente() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(solicitacaoService.indeferir(solicitante, 1, "parecer"))
                    .thenThrow(new IllegalStateException("Solicitação não está pendente"));

            assertThatThrownBy(() -> solicitacaoController.indeferir(1, "parecer", session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }
    }

    // reenviar (PATCH /api/solicitacao/reenviar/{id}) -------------------------

    @Nested
    class Reenviar {

        @Test
        void devePermitirReenvioQuandoDadosValidos() {
            Solicitacao esperado = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoService.reenviar(esperado.getId())).thenReturn(esperado);

            Solicitacao resultado = solicitacaoController.reenviar(esperado.getId());

            assertThat(resultado).isEqualTo(esperado);
            verify(solicitacaoService, times(1)).reenviar(esperado.getId());
        }

        @Test
        void devePropagarExcecaoQuandoSolicitacaoNaoExiste() {
            when(solicitacaoService.reenviar(999))
                    .thenThrow(new IllegalArgumentException("Solicitação não existe."));

            assertThatThrownBy(() -> solicitacaoController.reenviar(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitacaoNaoFoiIndeferida() {
            when(solicitacaoService.reenviar(1))
                    .thenThrow(new IllegalStateException("Solicitação não foi indeferida."));

            assertThatThrownBy(() -> solicitacaoController.reenviar(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não foi indeferida");
        }

        @Test
        void devePropagarExcecaoQuandoPrazoReenvioNaoDefinido() {
            when(solicitacaoService.reenviar(1))
                    .thenThrow(new IllegalStateException("Solicitação não possui prazo de reenvio definido."));

            assertThatThrownBy(() -> solicitacaoController.reenviar(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("prazo de reenvio");
        }

        // Nota: nem o endpoint nem SolicitacaoService.reenviar(Integer) recebem o
        // solicitante, então qualquer chamador pode reenviar a solicitação de
        // qualquer discente sabendo o ID. Sem HttpSession/Usuario na assinatura,
        // não há como expressar isso como um teste unitário.
    }

    // buscarPorId (GET /api/solicitacao/{id}) ---------------------------------

    @Nested
    class BuscarPorId {

        @Test
        void deveRetornarSolicitacaoQuandoExiste() {
            Solicitacao esperado = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoService.buscarPorId(esperado.getId())).thenReturn(esperado);

            Solicitacao resultado = solicitacaoController.buscarPorId(esperado.getId());

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveRetornarNuloQuandoNaoEncontrada() {
            when(solicitacaoService.buscarPorId(404)).thenReturn(null);

            Solicitacao resultado = solicitacaoController.buscarPorId(404);

            assertThat(resultado).isNull();
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            when(solicitacaoService.buscarPorId(null))
                    .thenThrow(new IllegalArgumentException("ID inválido."));

            assertThatThrownBy(() -> solicitacaoController.buscarPorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }
    }

    // listarPorDiscente (GET /api/solicitacao/discente/{id}) ------------------

    @Nested
    class ListarPorDiscente {

        @Test
        void deveRetornarSolicitacoesDoDiscente() {
            Discente d = discente();
            Solicitacao s1 = solicitacaoEntity(d, Status.PENDENTE, 20, LocalDate.now(), null, null);
            Solicitacao s2 = solicitacaoEntity(d, Status.APROVADO, 30, LocalDate.now(), null, null);
            when(solicitacaoService.listarPorDiscente(d.getId())).thenReturn(List.of(s1, s2));

            List<Solicitacao> resultado = solicitacaoController.listarPorDiscente(d.getId());

            assertThat(resultado).containsExactly(s1, s2);
        }

        @Test
        void deveRetornarListaVaziaQuandoDiscenteNaoTemSolicitacoes() {
            when(solicitacaoService.listarPorDiscente(1)).thenReturn(List.of());

            List<Solicitacao> resultado = solicitacaoController.listarPorDiscente(1);

            assertThat(resultado).isEmpty();
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            when(solicitacaoService.listarPorDiscente(999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> solicitacaoController.listarPorDiscente(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            when(solicitacaoService.listarPorDiscente(1))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> solicitacaoController.listarPorDiscente(1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }
    }

    // listarIndeferidos (GET /api/solicitacao/indeferidos/{id}) ---------------

    @Nested
    class ListarIndeferidos {

        @Test
        void deveRetornarSolicitacoesIndeferidas() {
            Discente d = discente();
            Solicitacao s1 = solicitacaoEntity(d, Status.INDEFERIDO, 20, LocalDate.now(), LocalDate.now().plusDays(5), "parecer");
            when(solicitacaoService.listarIndeferidos(d.getId())).thenReturn(List.of(s1));

            List<Solicitacao> resultado = solicitacaoController.listarIndeferidos(d.getId());

            assertThat(resultado).containsExactly(s1);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaIndeferidas() {
            when(solicitacaoService.listarIndeferidos(1)).thenReturn(List.of());

            List<Solicitacao> resultado = solicitacaoController.listarIndeferidos(1);

            assertThat(resultado).isEmpty();
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            // Aqui apenas verificamos o repasse do controller; a mensagem exata (com o
            // typo real do service) é coberta na seção de regras de negócio abaixo.
            when(solicitacaoService.listarIndeferidos(999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> solicitacaoController.listarIndeferidos(999))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            when(solicitacaoService.listarIndeferidos(1))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> solicitacaoController.listarIndeferidos(1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }
    }

    // listarPendentes (GET /api/solicitacao/pendentes) ------------------------

    @Nested
    class ListarPendentes {

        @Test
        void deveRetornarSolicitacoesPendentes() {
            Discente d = discente();
            Solicitacao s1 = solicitacaoEntity(d, Status.PENDENTE, 20, LocalDate.now(), null, null);
            Solicitacao s2 = solicitacaoEntity(d, Status.PENDENTE, 15, LocalDate.now(), null, null);
            when(solicitacaoService.listarPendentes()).thenReturn(List.of(s1, s2));

            List<Solicitacao> resultado = solicitacaoController.listarPendentes();

            assertThat(resultado).containsExactly(s1, s2);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaPendentes() {
            when(solicitacaoService.listarPendentes()).thenReturn(List.of());

            List<Solicitacao> resultado = solicitacaoController.listarPendentes();

            assertThat(resultado).isEmpty();
        }
    }

    // =========================================================================
    // ============  2) REGRAS DE NEGÓCIO REAIS (SolicitacaoService)  ========
    // =========================================================================

    @Nested
    class RegrasDeNegocioSubmeter {

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "2024-05-10", 999);

            assertThatThrownBy(() -> service.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
            verify(solicitacaoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "2024-05-10", docente.getId());

            assertThatThrownBy(() -> service.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
            verify(solicitacaoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoDataDeSolicitacaoEhNula() {
            Discente discente = discente();
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, null, discente.getId());

            assertThatThrownBy(() -> service.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de solicitação inválida");
        }

        @Test
        void deveLancarExcecaoQuandoDataDeSolicitacaoEhEmBranco() {
            Discente discente = discente();
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "   ", discente.getId());

            assertThatThrownBy(() -> service.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de solicitação inválida");
        }

        @Test
        void deveLancarExcecaoQuandoDataDeSolicitacaoEhInvalida() {
            Discente discente = discente();
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("desc", 20, "31/12/2024", discente.getId());

            assertThatThrownBy(() -> service.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de solicitação inválida");
        }

        @Test
        void devePermitirSubmissaoQuandoDadosValidos() {
            Discente discente = discente();
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("Participação em congresso", 20, "2024-05-10", discente.getId());

            Solicitacao resultado = service.submeter(dto);

            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
            assertThat(resultado.getDiscente()).isEqualTo(discente);
            assertThat(resultado.getCargaHorario()).isEqualTo(20);
            assertThat(resultado.getDataSolicitacao()).isEqualTo(LocalDate.parse("2024-05-10"));
        }

        @Test
        void deveRejeitarCargaHorariaNegativaNaSubmissao() {
            Discente discente = discente();
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("desc", -10, "2024-05-10", discente.getId());

            assertThatThrownBy(() -> service.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária");
            verify(solicitacaoRepo, never()).save(any());
        }

        @Test
        void deveRejeitarCargaHorariaNulaNaSubmissao() {
            Discente discente = discente();
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            SolicitacaoService service = novoServiceReal();
            SolicitacaoDTO dto = solicitacaoDTO("desc", null, "2024-05-10", discente.getId());

            assertThatThrownBy(() -> service.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária");
            verify(solicitacaoRepo, never()).save(any());
        }

        // Nota: submeter(SolicitacaoDTO) não valida DTO nulo, mas isso não é uma
        // falha real — o único chamador é SolicitacaoController.submeter(), cujo
        // parâmetro é @RequestBody (sem required=false), então o Spring já rejeita
        // requisições sem corpo antes de o controller ser invocado. Validar null
        // aqui seria defensividade sem chamador real que a exercite; por isso não
        // há teste para esse caso.
    }

    @Nested
    class RegrasDeNegocioAprovar {

        private Papel admin;
        private Papel coordenador;

        @BeforeEach
        void setUp() {
            admin = papel("ADMIN");
            coordenador = papel("COORDENADOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
        }

        @Test
        void deveAprovarQuandoAdminTemPermissao() {
            Discente discente = discente(10);
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente, Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            Solicitacao resultado = service.aprovar(solicitante, solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(discente.getCargaHoraria()).isEqualTo(30);
        }

        @Test
        void deveAprovarQuandoCoordenadorTemPermissao() {
            Discente discente = discente(0);
            Docente solicitante = docente(coordenador);
            Solicitacao solicitacao = solicitacaoEntity(discente, Status.PENDENTE, 15, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            Solicitacao resultado = service.aprovar(solicitante, solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(discente.getCargaHoraria()).isEqualTo(15);
        }

        @Test
        void deveTratarCargaHorariaAtualNulaComoZero() {
            Discente discente = discente(null);
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente, Status.PENDENTE, 25, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            service.aprovar(solicitante, solicitacao.getId());

            assertThat(discente.getCargaHoraria()).isEqualTo(25);
        }

        @Test
        void deveLancarSecurityExceptionQuandoSemPermissao() {
            Docente solicitante = docente(); // sem cargos ADMIN/COORDENADOR
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.aprovar(solicitante, solicitacao.getId()))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");
            verify(solicitacaoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
            Docente solicitante = docente(admin);
            when(solicitacaoRepo.findById(999)).thenReturn(Optional.empty());
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.aprovar(solicitante, 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoEstaPendente() {
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.APROVADO, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.aprovar(solicitante, solicitacao.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }

        // Sequência que viola invariante: aprovar duas vezes a mesma solicitação.
        @Test
        void deveLancarExcecaoAoTentarAprovarDuasVezes() {
            Docente solicitante = docente(admin);
            Discente discente = discente(0);
            Solicitacao solicitacao = solicitacaoEntity(discente, Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            service.aprovar(solicitante, solicitacao.getId());

            assertThatThrownBy(() -> service.aprovar(solicitante, solicitacao.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }

        @Test
        void deveRejeitarCargaHorariaNegativa() {
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.PENDENTE, -5, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.aprovar(solicitante, solicitacao.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária");
        }

        @Test
        void deveRejeitarCargaHorariaNula() {
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.PENDENTE, null, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.aprovar(solicitante, solicitacao.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária");
        }

        // Comportamento documentado (não é bug): a validação usa "< 0", então
        // cargaHoraria == 0 é aceita e a aprovação prossegue normalmente.
        @Test
        void devePermitirCargaHorariaZero() {
            Discente discente = discente(10);
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente, Status.PENDENTE, 0, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            Solicitacao resultado = service.aprovar(solicitante, solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(discente.getCargaHoraria()).isEqualTo(10);
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            SolicitacaoService service = novoServiceReal();
            Docente solicitante = docente(admin);

            assertThatThrownBy(() -> service.aprovar(solicitante, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }
    }

    @Nested
    class RegrasDeNegocioIndeferir {

        private Papel admin;
        private Papel coordenador;

        @BeforeEach
        void setUp() {
            admin = papel("ADMIN");
            coordenador = papel("COORDENADOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
        }

        @Test
        void deveIndeferirQuandoAdminTemPermissao() {
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            Solicitacao resultado = service.indeferir(solicitante, solicitacao.getId(), "Documentação incompleta");

            assertThat(resultado.getStatus()).isEqualTo(Status.INDEFERIDO);
            assertThat(resultado.getParecer()).isEqualTo("Documentação incompleta");
            assertThat(resultado.getPrazoReenvio()).isEqualTo(LocalDate.now().plusDays(5));
        }

        @Test
        void deveLancarExcecaoQuandoParecerEhNulo() {
            SolicitacaoService service = novoServiceReal();
            Docente solicitante = docente(admin);

            assertThatThrownBy(() -> service.indeferir(solicitante, 1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parecer inválido");
        }

        @Test
        void deveLancarExcecaoQuandoParecerEhEmBranco() {
            SolicitacaoService service = novoServiceReal();
            Docente solicitante = docente(admin);

            assertThatThrownBy(() -> service.indeferir(solicitante, 1, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parecer inválido");
        }

        @Test
        void deveLancarSecurityExceptionQuandoSemPermissao() {
            Docente solicitante = docente();
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.indeferir(solicitante, 1, "parecer válido"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
            Docente solicitante = docente(admin);
            when(solicitacaoRepo.findById(999)).thenReturn(Optional.empty());
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.indeferir(solicitante, 999, "parecer válido"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoEstaPendente() {
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.CANCELADO, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.indeferir(solicitante, solicitacao.getId(), "parecer válido"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }

        // Sequência que viola invariante: indeferir uma solicitação já indeferida.
        @Test
        void deveLancarExcecaoAoTentarIndeferirDuasVezes() {
            Docente solicitante = docente(admin);
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            service.indeferir(solicitante, solicitacao.getId(), "primeiro indeferimento");

            assertThatThrownBy(() -> service.indeferir(solicitante, solicitacao.getId(), "segunda tentativa"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }
    }

    @Nested
    class RegrasDeNegocioReenviar {

        @Test
        void deveReenviarQuandoDentroDoPrazo() {
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.INDEFERIDO, 20,
                    LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), "parecer anterior");
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            Solicitacao resultado = service.reenviar(solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
            assertThat(resultado.getParecer()).isNull();
            assertThat(resultado.getPrazoReenvio()).isNull();
        }

        // Limite exato: reenvio no próprio dia do prazo ainda deve ser permitido
        // (isBefore(hoje) é falso quando a data é igual a hoje).
        @Test
        void devePermitirReenvioNoUltimoDiaDoPrazo() {
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.INDEFERIDO, 20,
                    LocalDate.now().minusDays(5), LocalDate.now(), "parecer anterior");
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            Solicitacao resultado = service.reenviar(solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
        }

        @Test
        void deveCancelarQuandoPrazoJaExpirou() {
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.INDEFERIDO, 20,
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), "parecer anterior");
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));
            SolicitacaoService service = novoServiceReal();

            Solicitacao resultado = service.reenviar(solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.CANCELADO);
            assertThat(resultado.getParecer()).isEqualTo("O Aluno não fez o reenvio a tempo");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
            when(solicitacaoRepo.findById(999)).thenReturn(Optional.empty());
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.reenviar(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoFoiIndeferida() {
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.reenviar(solicitacao.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não foi indeferida");
        }

        // Estado inconsistente: status INDEFERIDO mas sem prazoReenvio definido.
        @Test
        void deveLancarExcecaoQuandoPrazoReenvioNaoDefinido() {
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.INDEFERIDO, 20, LocalDate.now(), null, "parecer");
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.reenviar(solicitacao.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("prazo de reenvio");
        }
    }

    @Nested
    class RegrasDeNegocioBuscarPorId {

        @Test
        void deveRetornarSolicitacaoQuandoExiste() {
            Solicitacao solicitacao = solicitacaoEntity(discente(), Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            SolicitacaoService service = novoServiceReal();

            assertThat(service.buscarPorId(solicitacao.getId())).isEqualTo(solicitacao);
        }

        @Test
        void deveRetornarNuloQuandoNaoEncontrada() {
            when(solicitacaoRepo.findById(404)).thenReturn(Optional.empty());
            SolicitacaoService service = novoServiceReal();

            assertThat(service.buscarPorId(404)).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.buscarPorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }
    }

    @Nested
    class RegrasDeNegocioListarPorDiscente {

        @Test
        void deveListarSolicitacoesDoDiscente() {
            Discente d = discente();
            Solicitacao s1 = solicitacaoEntity(d, Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(usuarioService.buscarPorId(d.getId())).thenReturn(d);
            when(solicitacaoRepo.findByDiscente(d)).thenReturn(List.of(s1));
            SolicitacaoService service = novoServiceReal();

            assertThat(service.listarPorDiscente(d.getId())).containsExactly(s1);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaSolicitacoes() {
            Discente d = discente();
            when(usuarioService.buscarPorId(d.getId())).thenReturn(d);
            when(solicitacaoRepo.findByDiscente(d)).thenReturn(List.of());
            SolicitacaoService service = novoServiceReal();

            assertThat(service.listarPorDiscente(d.getId())).isEmpty();
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.listarPorDiscente(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.listarPorDiscente(docente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }
    }

    @Nested
    class RegrasDeNegocioListarIndeferidos {

        @Test
        void deveListarSolicitacoesIndeferidasDoDiscente() {
            Discente d = discente();
            Solicitacao s1 = solicitacaoEntity(d, Status.INDEFERIDO, 20, LocalDate.now(), LocalDate.now().plusDays(5), "parecer");
            when(usuarioService.buscarPorId(d.getId())).thenReturn(d);
            when(solicitacaoRepo.findByDiscenteAndStatus(d, Status.INDEFERIDO)).thenReturn(List.of(s1));
            SolicitacaoService service = novoServiceReal();

            assertThat(service.listarIndeferidos(d.getId())).containsExactly(s1);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaIndeferidas() {
            Discente d = discente();
            when(usuarioService.buscarPorId(d.getId())).thenReturn(d);
            when(solicitacaoRepo.findByDiscenteAndStatus(d, Status.INDEFERIDO)).thenReturn(List.of());
            SolicitacaoService service = novoServiceReal();

            assertThat(service.listarIndeferidos(d.getId())).isEmpty();
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.listarIndeferidos(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);
            SolicitacaoService service = novoServiceReal();

            assertThatThrownBy(() -> service.listarIndeferidos(docente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }
    }

    @Nested
    class RegrasDeNegocioListarPendentes {

        @Test
        void deveListarSolicitacoesPendentes() {
            Discente d = discente();
            Solicitacao s1 = solicitacaoEntity(d, Status.PENDENTE, 20, LocalDate.now(), null, null);
            when(solicitacaoRepo.findByStatus(Status.PENDENTE)).thenReturn(List.of(s1));
            SolicitacaoService service = novoServiceReal();

            assertThat(service.listarPendentes()).containsExactly(s1);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaPendentes() {
            when(solicitacaoRepo.findByStatus(Status.PENDENTE)).thenReturn(List.of());
            SolicitacaoService service = novoServiceReal();

            assertThat(service.listarPendentes()).isEmpty();
        }
    }
}