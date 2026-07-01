package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.service.OportunidadeService;
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
import static org.mockito.Mockito.*;

/*
 * Mesmo padrão de CursoControllerTest/GrupoControllerTest: OportunidadeService é
 * mockado e a sessão é resolvida por Sessao.logado(session, usuarioService).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OportunidadeControllerTest {

    @Mock
    OportunidadeService oportunidadeService;

    @Mock
    UsuarioService usuarioService;

    @Mock
    HttpSession session;

    @InjectMocks
    OportunidadeController oportunidadeController;

    private int nextId = 1;

    private Docente docente() {
        Docente d = Docente.builder()
                .nome("Carlos")
                .email("carlos@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>())
                .build();
        d.setId(nextId++);
        return d;
    }

    private Oportunidade oportunidade(StatusOp status, Docente coordenador) {
        Oportunidade o = Oportunidade.builder()
                .titulo("Monitoria de Cálculo")
                .descricao("Auxílio aos discentes de Cálculo I")
                .cargaHoraria(60)
                .vagas(2)
                .vagasLivres(2)
                .status(status)
                .coordenador(coordenador)
                .build();
        o.setId(nextId++);
        return o;
    }

    private OportunidadeDTO oportunidadeDTO(String titulo, Integer cargaHoraria, String tipo, Integer idDocente) {
        return OportunidadeDTO.builder()
                .titulo(titulo)
                .descricao("Descrição da oportunidade")
                .cargaHoraria(cargaHoraria)
                .vagas(2)
                .tipo(tipo)
                .idDocente(idDocente)
                .build();
    }

    /** Simula um usuário autenticado na sessão. */
    private void logarComo(Usuario usuario) {
        when(session.getAttribute("IdUsuarioLogado")).thenReturn(usuario.getId());
        when(usuarioService.buscarPorId(usuario.getId())).thenReturn(usuario);
    }

    // criaOportunidade (POST /api/oportunidade/criar) ------------------------

    @Nested
    class CriaOportunidade {

        @Test
        void devePermitirCriacaoQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            logarComo(solicitante);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria de Cálculo", 60, "MONITORIA", solicitante.getId());
            Oportunidade esperada = oportunidade(StatusOp.RASCUNHO, solicitante);
            when(oportunidadeService.criaOportunidade(solicitante, dto)).thenReturn(esperada);

            Oportunidade resultado = oportunidadeController.criaOportunidade(dto, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(oportunidadeService, times(1)).criaOportunidade(solicitante, dto);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", 60, "MONITORIA", 1);

            assertThatThrownBy(() -> oportunidadeController.criaOportunidade(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(oportunidadeService, never()).criaOportunidade(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", 60, "MONITORIA", 1);

            assertThatThrownBy(() -> oportunidadeController.criaOportunidade(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(oportunidadeService, never()).criaOportunidade(any(), any());
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteSemPermissao() {
            Docente solicitante = docente();
            logarComo(solicitante);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", 60, "MONITORIA", solicitante.getId());
            when(oportunidadeService.criaOportunidade(solicitante, dto))
                    .thenThrow(new SecurityException("O solicitante não possui permissão."));

            assertThatThrownBy(() -> oportunidadeController.criaOportunidade(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");
        }

        @Test
        void devePropagarExcecaoQuandoTituloEmBranco() {
            Docente solicitante = docente();
            logarComo(solicitante);
            OportunidadeDTO dto = oportunidadeDTO("", 60, "MONITORIA", solicitante.getId());
            when(oportunidadeService.criaOportunidade(solicitante, dto))
                    .thenThrow(new IllegalArgumentException("Título é obrigatório."));

            assertThatThrownBy(() -> oportunidadeController.criaOportunidade(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Título é obrigatório.");
        }

        @Test
        void devePropagarExcecaoQuandoTipoNaoExiste() {
            Docente solicitante = docente();
            logarComo(solicitante);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", 60, "INEXISTENTE", solicitante.getId());
            when(oportunidadeService.criaOportunidade(solicitante, dto))
                    .thenThrow(new IllegalArgumentException("Tipo não existe."));

            assertThatThrownBy(() -> oportunidadeController.criaOportunidade(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tipo não existe.");
        }
    }

    // publicarOportunidade (PATCH /api/oportunidade/publicar/{id}) -----------

    @Nested
    class PublicarOportunidade {

        @Test
        void devePermitirPublicacaoQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Oportunidade esperada = oportunidade(StatusOp.ABERTA, solicitante);
            when(oportunidadeService.publicarOportunidade(1, solicitante)).thenReturn(esperada);

            Oportunidade resultado = oportunidadeController.publicarOportunidade(1, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(oportunidadeService, times(1)).publicarOportunidade(1, solicitante);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> oportunidadeController.publicarOportunidade(1, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(oportunidadeService, never()).publicarOportunidade(any(), any());
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoExiste() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(oportunidadeService.publicarOportunidade(999, solicitante))
                    .thenThrow(new IllegalArgumentException("Oportunidade não encontrada."));

            assertThatThrownBy(() -> oportunidadeController.publicarOportunidade(999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada.");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteSemPermissao() {
            Discente solicitante = Discente.builder().nome("Joana").email("joana@ufma.br").senha("hash")
                    .ativo(true).cargos(new ArrayList<>()).build();
            solicitante.setId(nextId++);
            logarComo(solicitante);
            when(oportunidadeService.publicarOportunidade(1, solicitante))
                    .thenThrow(new SecurityException("O solicitante não possui permissão."));

            assertThatThrownBy(() -> oportunidadeController.publicarOportunidade(1, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");
        }
    }

    // aprovarOportunidade (PATCH /api/oportunidade/aprovar/{id}) -------------

    @Nested
    class AprovarOportunidade {

        @Test
        void devePermitirAprovacaoQuandoAdminEstaLogado() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Oportunidade esperada = oportunidade(StatusOp.ABERTA, solicitante);
            when(oportunidadeService.aprovarOportunidade(1, solicitante)).thenReturn(esperada);

            Oportunidade resultado = oportunidadeController.aprovarOportunidade(1, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(oportunidadeService, times(1)).aprovarOportunidade(1, solicitante);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> oportunidadeController.aprovarOportunidade(1, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(oportunidadeService, never()).aprovarOportunidade(any(), any());
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoEstaAguardandoAprovacao() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(oportunidadeService.aprovarOportunidade(1, solicitante))
                    .thenThrow(new IllegalStateException("Oportunidade deve está aguardando aprovação."));

            assertThatThrownBy(() -> oportunidadeController.aprovarOportunidade(1, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("aguardando aprovação");
        }
    }

    // iniciarOportunidade (PATCH /api/oportunidade/iniciar/{id}) -------------

    @Nested
    class IniciarOportunidade {

        @Test
        void devePermitirInicioQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Oportunidade esperada = oportunidade(StatusOp.EM_EXECUCAO, solicitante);
            when(oportunidadeService.iniciarOportunidade(1, solicitante)).thenReturn(esperada);

            Oportunidade resultado = oportunidadeController.iniciarOportunidade(1, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(oportunidadeService, times(1)).iniciarOportunidade(1, solicitante);
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoEstaAberta() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(oportunidadeService.iniciarOportunidade(1, solicitante))
                    .thenThrow(new IllegalStateException("Oportunidade deve está aberta."));

            assertThatThrownBy(() -> oportunidadeController.iniciarOportunidade(1, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deve está aberta");
        }
    }

    // encerrarOportunidade (PATCH /api/oportunidade/encerrar/{id}) -----------

    @Nested
    class EncerrarOportunidade {

        @Test
        void devePermitirEncerramentoQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Oportunidade esperada = oportunidade(StatusOp.ENCERRADA, solicitante);
            when(oportunidadeService.encerrarOportunidade(1, solicitante)).thenReturn(esperada);

            Oportunidade resultado = oportunidadeController.encerrarOportunidade(1, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(oportunidadeService, times(1)).encerrarOportunidade(1, solicitante);
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoEstaEmExecucao() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(oportunidadeService.encerrarOportunidade(1, solicitante))
                    .thenThrow(new IllegalStateException("Oportunidade deve está em execução."));

            assertThatThrownBy(() -> oportunidadeController.encerrarOportunidade(1, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("em execução");
        }
    }

    // cancelarOportunidade (PATCH /api/oportunidade/cancelar/{id}) -----------

    @Nested
    class CancelarOportunidade {

        @Test
        void devePermitirCancelamentoQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Oportunidade esperada = oportunidade(StatusOp.CANCELADA, solicitante);
            when(oportunidadeService.cancelarOportunidade(1, solicitante)).thenReturn(esperada);

            Oportunidade resultado = oportunidadeController.cancelarOportunidade(1, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(oportunidadeService, times(1)).cancelarOportunidade(1, solicitante);
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoExiste() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(oportunidadeService.cancelarOportunidade(999, solicitante))
                    .thenThrow(new IllegalArgumentException("Oportunidade não encontrada."));

            assertThatThrownBy(() -> oportunidadeController.cancelarOportunidade(999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada.");
        }
    }

    // listarOportunidades (GET /api/oportunidade/oportunidade) ---------------

    @Nested
    class ListarOportunidades {

        @Test
        void deveRetornarTodasAsOportunidades() {
            Oportunidade o = oportunidade(StatusOp.ABERTA, docente());
            when(oportunidadeService.listarOportunidades()).thenReturn(List.of(o));

            List<Oportunidade> resultado = oportunidadeController.listarOportunidades();

            assertThat(resultado).containsExactly(o);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaOportunidades() {
            when(oportunidadeService.listarOportunidades()).thenReturn(List.of());

            assertThat(oportunidadeController.listarOportunidades()).isEmpty();
        }
    }
}
