package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.service.InscricaoService;
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
 * Mesmo padrão de CursoControllerTest/GrupoControllerTest: InscricaoService é
 * mockado e a sessão é resolvida por Sessao.logado(session, usuarioService).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscricaoControllerTest {

    @Mock
    InscricaoService inscricaoService;

    @Mock
    UsuarioService usuarioService;

    @Mock
    HttpSession session;

    @InjectMocks
    InscricaoController inscricaoController;

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

    private Inscricao inscricao(Integer id) {
        Inscricao i = new Inscricao();
        i.setId(id);
        return i;
    }

    private InscricaoDTO inscricaoDTO(Integer idOportunidade, Integer idDiscente, String motivacao) {
        InscricaoDTO dto = new InscricaoDTO();
        dto.setIdOportunidade(idOportunidade);
        dto.setIdDiscente(idDiscente);
        dto.setMotivacao(motivacao);
        return dto;
    }

    /** Simula um usuário autenticado na sessão. */
    private void logarComo(Usuario usuario) {
        when(session.getAttribute("IdUsuarioLogado")).thenReturn(usuario.getId());
        when(usuarioService.buscarPorId(usuario.getId())).thenReturn(usuario);
    }

    // inscrever (POST /api/inscricao/inscrever) -----------------------------

    @Nested
    class Inscrever {

        @Test
        void devePermitirInscricaoComDadosValidos() {
            InscricaoDTO dto = inscricaoDTO(5, 1, "Interesse na área");
            Inscricao esperada = inscricao(1);
            when(inscricaoService.inscrever(dto)).thenReturn(esperada);

            Inscricao resultado = inscricaoController.inscrever(dto);

            assertThat(resultado).isEqualTo(esperada);
            verify(inscricaoService, times(1)).inscrever(dto);
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoExiste() {
            InscricaoDTO dto = inscricaoDTO(999, 1, "Interesse");
            when(inscricaoService.inscrever(dto))
                    .thenThrow(new IllegalArgumentException("Oportunidade não existente"));

            assertThatThrownBy(() -> inscricaoController.inscrever(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não existente");
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoEstaAberta() {
            InscricaoDTO dto = inscricaoDTO(5, 1, "Interesse");
            when(inscricaoService.inscrever(dto))
                    .thenThrow(new IllegalStateException("Oportunidade não está aberta para inscrições."));

            assertThatThrownBy(() -> inscricaoController.inscrever(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está aberta");
        }
    }

    // aprovar (PATCH /api/inscricao/aprovar/{id}) ----------------------------

    @Nested
    class Aprovar {

        @Test
        void devePermitirAprovacaoQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Inscricao esperada = inscricao(1);
            when(inscricaoService.aprovar(1, solicitante)).thenReturn(esperada);

            Inscricao resultado = inscricaoController.aprovar(1, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(inscricaoService, times(1)).aprovar(1, solicitante);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> inscricaoController.aprovar(1, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(inscricaoService, never()).aprovar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> inscricaoController.aprovar(1, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(inscricaoService, never()).aprovar(any(), any());
        }

        @Test
        void devePropagarExcecaoQuandoInscricaoNaoExiste() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(inscricaoService.aprovar(999, solicitante))
                    .thenThrow(new IllegalArgumentException("Inscrição não existe"));

            assertThatThrownBy(() -> inscricaoController.aprovar(999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Inscrição não existe");
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeSemVagas() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(inscricaoService.aprovar(1, solicitante))
                    .thenThrow(new IllegalStateException("A oportunidade não possui vagas livres."));

            assertThatThrownBy(() -> inscricaoController.aprovar(1, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("vagas livres");
        }
    }

    // rejeitar (PATCH /api/inscricao/rejeitar/{id}) --------------------------

    @Nested
    class Rejeitar {

        @Test
        void devePermitirRejeicaoComJustificativa() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Inscricao esperada = inscricao(1);
            when(inscricaoService.rejeitar(1, "Fora do prazo", solicitante)).thenReturn(esperada);

            Inscricao resultado = inscricaoController.rejeitar(1, "Fora do prazo", session);

            assertThat(resultado).isEqualTo(esperada);
            verify(inscricaoService, times(1)).rejeitar(1, "Fora do prazo", solicitante);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> inscricaoController.rejeitar(1, "motivo", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(inscricaoService, never()).rejeitar(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> inscricaoController.rejeitar(1, "motivo", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(inscricaoService, never()).rejeitar(any(), any(), any());
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoEhResponsavel() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(inscricaoService.rejeitar(1, "motivo", solicitante))
                    .thenThrow(new SecurityException("Apenas o responsável pode fazer isso."));

            assertThatThrownBy(() -> inscricaoController.rejeitar(1, "motivo", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Apenas o responsável");
        }

        @Test
        void devePropagarExcecaoQuandoInscricaoNaoEstaPendente() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(inscricaoService.rejeitar(1, "motivo", solicitante))
                    .thenThrow(new IllegalStateException("Só é possível rejeitar inscrições PENDENTES."));

            assertThatThrownBy(() -> inscricaoController.rejeitar(1, "motivo", session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDENTES");
        }
    }

    // remover (PATCH /api/inscricao/remover/{id}) ----------------------------

    @Nested
    class Remover {

        @Test
        void devePermitirRemocaoComJustificativa() {
            Docente solicitante = docente();
            logarComo(solicitante);
            Inscricao esperada = inscricao(1);
            when(inscricaoService.removerDiscente(1, "Descumpriu regras", solicitante)).thenReturn(esperada);

            Inscricao resultado = inscricaoController.remover(1, "Descumpriu regras", session);

            assertThat(resultado).isEqualTo(esperada);
            verify(inscricaoService, times(1)).removerDiscente(1, "Descumpriu regras", solicitante);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> inscricaoController.remover(1, "motivo", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(inscricaoService, never()).removerDiscente(any(), any(), any());
        }

        @Test
        void devePropagarExcecaoQuandoInscricaoNaoEstaAprovada() {
            Docente solicitante = docente();
            logarComo(solicitante);
            when(inscricaoService.removerDiscente(1, "motivo", solicitante))
                    .thenThrow(new IllegalStateException("Só é possível retirar inscrições aprovadas."));

            assertThatThrownBy(() -> inscricaoController.remover(1, "motivo", session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("aprovadas");
        }
    }

    // desistir (PATCH /api/inscricao/desistir/{id}) --------------------------

    @Nested
    class Desistir {

        @Test
        void devePermitirDesistenciaDoProprioDiscente() {
            Discente solicitante = discente();
            logarComo(solicitante);
            Inscricao esperada = inscricao(1);
            when(inscricaoService.desistir(1, solicitante)).thenReturn(esperada);

            Inscricao resultado = inscricaoController.desistir(1, session);

            assertThat(resultado).isEqualTo(esperada);
            verify(inscricaoService, times(1)).desistir(1, solicitante);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> inscricaoController.desistir(1, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(inscricaoService, never()).desistir(any(), any());
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoEhODiscenteDaInscricao() {
            Discente solicitante = discente();
            logarComo(solicitante);
            when(inscricaoService.desistir(1, solicitante))
                    .thenThrow(new IllegalStateException("Apenas o próprio discente pode desistir"));

            assertThatThrownBy(() -> inscricaoController.desistir(1, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("próprio discente");
        }
    }

    // listarPorOportunidade (GET /api/inscricao/lista/oportunidade/{id}) -----

    @Nested
    class ListarPorOportunidade {

        @Test
        void deveRetornarInscricoesDaOportunidade() {
            Inscricao i1 = inscricao(1);
            when(inscricaoService.listarPorOportunidade(5)).thenReturn(List.of(i1));

            List<Inscricao> resultado = inscricaoController.listarPorOportunidade(5);

            assertThat(resultado).containsExactly(i1);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaInscricoes() {
            when(inscricaoService.listarPorOportunidade(5)).thenReturn(List.of());

            assertThat(inscricaoController.listarPorOportunidade(5)).isEmpty();
        }

        @Test
        void devePropagarExcecaoQuandoOportunidadeNaoExiste() {
            when(inscricaoService.listarPorOportunidade(999))
                    .thenThrow(new IllegalArgumentException("Oportunidade não existe."));

            assertThatThrownBy(() -> inscricaoController.listarPorOportunidade(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não existe.");
        }
    }

    // listarFilaEspera (GET /api/inscricao/lista/fila-espera/{id}) ----------

    @Nested
    class ListarFilaEspera {

        @Test
        void deveRetornarFilaDeEspera() {
            Inscricao i1 = inscricao(2);
            when(inscricaoService.listarFilaEspera(5)).thenReturn(List.of(i1));

            assertThat(inscricaoController.listarFilaEspera(5)).containsExactly(i1);
        }

        @Test
        void deveRetornarListaVaziaQuandoFilaVazia() {
            when(inscricaoService.listarFilaEspera(5)).thenReturn(List.of());

            assertThat(inscricaoController.listarFilaEspera(5)).isEmpty();
        }
    }

    // listarPorDiscente (GET /api/inscricao/lista/discente/{id}) ------------

    @Nested
    class ListarPorDiscente {

        @Test
        void deveRetornarInscricoesDoDiscente() {
            Inscricao i1 = inscricao(3);
            when(inscricaoService.listarPorDiscente(7)).thenReturn(List.of(i1));

            assertThat(inscricaoController.listarPorDiscente(7)).containsExactly(i1);
        }

        @Test
        void deveRetornarListaVaziaQuandoDiscenteSemInscricoes() {
            when(inscricaoService.listarPorDiscente(7)).thenReturn(List.of());

            assertThat(inscricaoController.listarPorDiscente(7)).isEmpty();
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            when(inscricaoService.listarPorDiscente(1))
                    .thenThrow(new IllegalArgumentException("Usuário precisa ser discente."));

            assertThatThrownBy(() -> inscricaoController.listarPorDiscente(1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("precisa ser discente");
        }
    }
}