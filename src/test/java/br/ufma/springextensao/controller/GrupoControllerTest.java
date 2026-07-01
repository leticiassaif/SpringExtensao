package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.service.GrupoService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/*
 * NOTA: GrupoController é um passthrough puro para GrupoService (via Sessao.logado
 * para autenticação). Por isso este teste cobre: (1) resolução de sessão/login,
 * (2) delegação com os parâmetros corretos na ordem correta, e (3) propagação de
 * toda exceção que o GrupoService declara para cada método (simulada via mock).
 * Bugs de lógica *interna* do GrupoService (ex.: releitura de estado obsoleto do
 * banco, comparação case-sensitive de cargo restrito) não são visíveis aqui porque
 * o service é totalmente mockado — eles exigem um GrupoServiceTest à parte. Ver
 * relatório de bugs ao final da resposta.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrupoControllerTest {

    @Mock
    GrupoService grupoService;

    @Mock
    UsuarioService usuarioService;

    @Mock
    HttpSession session;

    @InjectMocks
    GrupoController grupoController;

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

    private Grupo grupo(Status status, Docente responsavel) {
        Grupo g = Grupo.builder()
                .nome("PET Computação")
                .descricao("Grupo de pesquisa e extensão")
                .email("pet@ufma.br")
                .status(status)
                .responsavel(responsavel)
                .membros(new ArrayList<>())
                .membrosHistorico(new ArrayList<>())
                .build();
        g.setId(nextId++);
        return g;
    }

    private GrupoDTO grupoDTO(String nome, String descricao, String email,
                              Integer idCurso, Integer idResponsavel) {
        GrupoDTO dto = new GrupoDTO();
        dto.setNome(nome);
        dto.setDescricao(descricao);
        dto.setEmail(email);
        dto.setIdCurso(idCurso);
        dto.setIdResponsavel(idResponsavel);
        return dto;
    }

    /** Simula um usuário autenticado na sessão. */
    private void logarComo(Usuario usuario) {
        when(session.getAttribute("IdUsuarioLogado")).thenReturn(usuario.getId());
        when(usuarioService.buscarPorId(usuario.getId())).thenReturn(usuario);
    }

    // criarGrupo (POST /api/usuario/criar) -----------------------------

    @Nested
    class CriarGrupo {

        @Test
        void devePermitirCriacaoQuandoDocenteEstaLogado() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            GrupoDTO dto = grupoDTO("PET Computação", "Extensão", "pet@ufma.br", 1, solicitante.getId());
            Grupo esperado = grupo(Status.PENDENTE, solicitante);
            when(grupoService.criar(dto, solicitante)).thenReturn(esperado);

            Grupo resultado = grupoController.criarGrupo(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).criar(dto, solicitante);
        }

        @Test
        void devePermitirCriacaoQuandoDiscenteEstaLogado() {
            // O controller não filtra por papel; quem decide permissão é o service.
            Discente solicitante = discente();
            logarComo(solicitante);
            Docente responsavel = docente(papel("ADMIN"));
            GrupoDTO dto = grupoDTO("Liga de IA", "Extensão", "liga@ufma.br", 1, responsavel.getId());
            Grupo esperado = grupo(Status.PENDENTE, responsavel);
            when(grupoService.criar(dto, solicitante)).thenReturn(esperado);

            Grupo resultado = grupoController.criarGrupo(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).criar(dto, solicitante);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);
            GrupoDTO dto = grupoDTO("Grupo", "Desc", "g@ufma.br", 1, 2);

            assertThatThrownBy(() -> grupoController.criarGrupo(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(grupoService, never()).criar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            GrupoDTO dto = grupoDTO("Grupo", "Desc", "g@ufma.br", 1, 2);

            assertThatThrownBy(() -> grupoController.criarGrupo(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(grupoService, never()).criar(any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");
            GrupoDTO dto = grupoDTO("Grupo", "Desc", "g@ufma.br", 1, 2);

            assertThatThrownBy(() -> grupoController.criarGrupo(dto, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoDtoEhNulo() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            when(grupoService.criar(null, solicitante))
                    .thenThrow(new IllegalArgumentException("Dados do grupo inválidos."));

            assertThatThrownBy(() -> grupoController.criarGrupo(null, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Dados do grupo inválidos.");

            verify(grupoService, times(1)).criar(null, solicitante);
        }

        @Test
        void devePropagarExcecaoQuandoResponsavelNaoExiste() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            GrupoDTO dto = grupoDTO("Grupo", "Desc", "g@ufma.br", 1, 999);
            when(grupoService.criar(dto, solicitante))
                    .thenThrow(new IllegalArgumentException("Usuário(s) não existe."));

            assertThatThrownBy(() -> grupoController.criarGrupo(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário(s) não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoResponsavelNaoEhDocente() {
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            Discente naoDocente = discente();
            GrupoDTO dto = grupoDTO("Grupo", "Desc", "g@ufma.br", 1, naoDocente.getId());
            when(grupoService.criar(dto, solicitante))
                    .thenThrow(new IllegalArgumentException("Usuário não é docente."));

            assertThatThrownBy(() -> grupoController.criarGrupo(dto, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é docente.");
        }

        @Test
        void devePermitirDtoComCamposEmBrancoRepassandoParaOService() {
            // O controller não faz nenhuma validação de conteúdo; qualquer checagem
            // de nome/descrição/email em branco é responsabilidade do service.
            // Aqui só provamos que o controller repassa o DTO como está, sem alterar
            // nem rejeitar campos em branco por conta própria.
            Docente solicitante = docente(papel("ADMIN"));
            logarComo(solicitante);
            GrupoDTO dto = grupoDTO("", "   ", "", null, solicitante.getId());
            Grupo esperado = grupo(Status.PENDENTE, solicitante);
            when(grupoService.criar(dto, solicitante)).thenReturn(esperado);

            Grupo resultado = grupoController.criarGrupo(dto, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).criar(dto, solicitante);
        }
    }

    // aprovar (PATCH /api/usuario/aprovar/{idGrupo}) --------------------

    @Nested
    class Aprovar {

        @Test
        void devePermitirAprovacaoQuandoAdminEstaLogado() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            Grupo grupoPendente = grupo(Status.PENDENTE, docente());
            Grupo esperado = grupo(Status.APROVADO, grupoPendente.getResponsavel());
            when(grupoService.aprovar(admin, grupoPendente.getId(), 42)).thenReturn(esperado);

            Grupo resultado = grupoController.aprovar(grupoPendente.getId(), 42, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).aprovar(admin, grupoPendente.getId(), 42);
        }

        @Test
        void devePermitirAprovacaoQuandoResponsavelDoGrupoEstaLogado() {
            // O controller não checa se quem está logado tem permissão; isso é
            // responsabilidade do service.
            Docente responsavel = docente();
            logarComo(responsavel);
            Grupo esperado = grupo(Status.APROVADO, responsavel);
            when(grupoService.aprovar(responsavel, 10, 42)).thenReturn(esperado);

            Grupo resultado = grupoController.aprovar(10, 42, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).aprovar(responsavel, 10, 42);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> grupoController.aprovar(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(grupoService, never()).aprovar(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> grupoController.aprovar(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(grupoService, never()).aprovar(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> grupoController.aprovar(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoExiste() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.aprovar(admin, 999, 42))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.aprovar(999, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.aprovar(admin, 10, 999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> grupoController.aprovar(10, 999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.aprovar(admin, 10, 42))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> grupoController.aprovar(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é discente.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoEstaPendente() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.aprovar(admin, 10, 42))
                    .thenThrow(new IllegalStateException("Grupo não está pendente"));

            assertThatThrownBy(() -> grupoController.aprovar(10, 42, session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Grupo não está pendente");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Discente semPermissao = discente();
            logarComo(semPermissao);
            when(grupoService.aprovar(semPermissao, 10, 42))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> grupoController.aprovar(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não possui permissão.");
        }

        @Test
        void devePropagarExcecaoQuandoIdGrupoEhNegativo() {
            // O controller não valida o formato do id; quem valida é o service.
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.aprovar(admin, -1, 42))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.aprovar(-1, 42, session))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(grupoService, times(1)).aprovar(admin, -1, 42);
        }
    }

    // rejeitar (PATCH /api/usuario/rejeitar/{id}) -----------------------

    @Nested
    class Rejeitar {

        @Test
        void devePermitirRejeicaoQuandoAdminEstaLogado() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            Grupo esperado = grupo(Status.REJEITADO, docente());
            when(grupoService.rejeitar(admin, 10, "Fora do prazo")).thenReturn(esperado);

            Grupo resultado = grupoController.rejeitar(10, "Fora do prazo", session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).rejeitar(admin, 10, "Fora do prazo");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> grupoController.rejeitar(10, "motivo", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(grupoService, never()).rejeitar(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> grupoController.rejeitar(10, "motivo", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(grupoService, never()).rejeitar(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> grupoController.rejeitar(10, "motivo", session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Discente semPermissao = discente();
            logarComo(semPermissao);
            when(grupoService.rejeitar(semPermissao, 10, "motivo"))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> grupoController.rejeitar(10, "motivo", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não possui permissão.");
        }

        @Test
        void devePropagarExcecaoQuandoJustificativaEhNula() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.rejeitar(admin, 10, null))
                    .thenThrow(new IllegalArgumentException("Justificativa é obrigatória."));

            assertThatThrownBy(() -> grupoController.rejeitar(10, null, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Justificativa é obrigatória.");
        }

        @Test
        void devePropagarExcecaoQuandoJustificativaEhVazia() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.rejeitar(admin, 10, ""))
                    .thenThrow(new IllegalArgumentException("Justificativa é obrigatória."));

            assertThatThrownBy(() -> grupoController.rejeitar(10, "", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Justificativa é obrigatória.");
        }

        @Test
        void devePropagarExcecaoQuandoJustificativaEhEmBranco() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.rejeitar(admin, 10, "   "))
                    .thenThrow(new IllegalArgumentException("Justificativa é obrigatória."));

            assertThatThrownBy(() -> grupoController.rejeitar(10, "   ", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Justificativa é obrigatória.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoExiste() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.rejeitar(admin, 999, "motivo"))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.rejeitar(999, "motivo", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoEstaPendente() {
            Docente admin = docente(papel("ADMIN"));
            logarComo(admin);
            when(grupoService.rejeitar(admin, 10, "motivo"))
                    .thenThrow(new IllegalStateException("Grupo não está pendente"));

            assertThatThrownBy(() -> grupoController.rejeitar(10, "motivo", session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Grupo não está pendente");
        }
    }

    // adicionarMembro (PATCH /api/usuario/addmembro/{idGrupo}/{idDiscente}) --

    @Nested
    class AdicionarMembro {

        @Test
        void devePermitirAdicaoQuandoSolicitanteTemPermissao() {
            Docente responsavel = docente();
            logarComo(responsavel);
            Grupo esperado = grupo(Status.APROVADO, responsavel);
            when(grupoService.adicionarMembro(responsavel, 10, 42)).thenReturn(esperado);

            Grupo resultado = grupoController.adicionarMembro(10, 42, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).adicionarMembro(responsavel, 10, 42);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(grupoService, never()).adicionarMembro(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(grupoService, never()).adicionarMembro(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.adicionarMembro(responsavel, 10, 999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.adicionarMembro(responsavel, 10, 42))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é discente.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.adicionarMembro(responsavel, 999, 42))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(999, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Discente semPermissao = discente();
            logarComo(semPermissao);
            when(grupoService.adicionarMembro(semPermissao, 10, 42))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não possui permissão.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoEstaAprovado() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.adicionarMembro(responsavel, 10, 42))
                    .thenThrow(new IllegalArgumentException("Grupo precisa estar ativo/aprovado."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo precisa estar ativo/aprovado.");
        }

        @Test
        void devePropagarExcecaoQuandoDiscenteJaFazParteDoGrupo() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.adicionarMembro(responsavel, 10, 42))
                    .thenThrow(new IllegalArgumentException("O discente já faz parte do grupo."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O discente já faz parte do grupo.");
        }

        @Test
        void devePropagarExcecaoQuandoDiscenteNaoEstaAtivo() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.adicionarMembro(responsavel, 10, 42))
                    .thenThrow(new IllegalArgumentException("O novo membro precisa ser um usuário ativo."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O novo membro precisa ser um usuário ativo.");
        }

        @Test
        void devePermitirIdGrupoEIdDiscenteIguaisRepassandoParaOService() {
            // Caso de borda: idGrupo e idDiscente coincidindo numericamente (ids são
            // sequências independentes). O controller não tem como saber que isso é
            // suspeito; só o service pode validar. Aqui só garantimos o repasse fiel.
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.adicionarMembro(responsavel, 5, 5))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> grupoController.adicionarMembro(5, 5, session))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(grupoService, times(1)).adicionarMembro(responsavel, 5, 5);
        }
    }

    // removerMembro (PATCH /api/usuario/removemembro/{idGrupo}/{idDiscente}) -

    @Nested
    class RemoverMembro {

        @Test
        void devePermitirRemocaoQuandoSolicitanteTemPermissao() {
            Docente responsavel = docente();
            logarComo(responsavel);
            Grupo esperado = grupo(Status.APROVADO, responsavel);
            when(grupoService.removerMembro(responsavel, 10, 42)).thenReturn(esperado);

            Grupo resultado = grupoController.removerMembro(10, 42, session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).removerMembro(responsavel, 10, 42);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> grupoController.removerMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(grupoService, never()).removerMembro(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> grupoController.removerMembro(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(grupoService, never()).removerMembro(any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> grupoController.removerMembro(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerMembro(responsavel, 999, 42))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.removerMembro(999, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Discente semPermissao = discente();
            logarComo(semPermissao);
            when(grupoService.removerMembro(semPermissao, 10, 42))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> grupoController.removerMembro(10, 42, session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não possui permissão.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoEstaAprovado() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerMembro(responsavel, 10, 42))
                    .thenThrow(new IllegalArgumentException("Grupo precisa estar ativo/aprovado."));

            assertThatThrownBy(() -> grupoController.removerMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo precisa estar ativo/aprovado.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerMembro(responsavel, 10, 999))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> grupoController.removerMembro(10, 999, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerMembro(responsavel, 10, 42))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> grupoController.removerMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é discente.");
        }

        @Test
        void devePropagarExcecaoQuandoDiscenteNaoFazParteDoGrupo() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerMembro(responsavel, 10, 42))
                    .thenThrow(new IllegalArgumentException("O discente não faz parte do grupo."));

            assertThatThrownBy(() -> grupoController.removerMembro(10, 42, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O discente não faz parte do grupo.");
        }
    }

    // atribuirCargo (PATCH /api/usuario/atribuircargo/{idGrupo}/{idDiscente}) -

    @Nested
    class AtribuirCargo {

        @Test
        void devePermitirAtribuicaoQuandoSolicitanteTemPermissao() {
            Docente responsavel = docente();
            logarComo(responsavel);
            Grupo esperado = grupo(Status.APROVADO, responsavel);
            // Ordem real esperada pelo service: (solicitante, idDiscente, idGrupo, cargo)
            when(grupoService.atribuirCargo(responsavel, 42, 10, "TESOUREIRO")).thenReturn(esperado);

            Grupo resultado = grupoController.atribuirCargo(10, 42, "TESOUREIRO", session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).atribuirCargo(responsavel, 42, 10, "TESOUREIRO");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(grupoService, never()).atribuirCargo(any(), any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(grupoService, never()).atribuirCargo(any(), any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoCargoEhNulo() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 10, null))
                    .thenThrow(new IllegalArgumentException("Cargo inválido."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, null, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido.");
        }

        @Test
        void devePropagarExcecaoQuandoCargoEhEmBranco() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 10, "   "))
                    .thenThrow(new IllegalArgumentException("Cargo inválido."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "   ", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido.");
        }

        @Test
        void devePropagarExcecaoQuandoCargoNaoPodeSerAtribuido() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 10, "ADMIN"))
                    .thenThrow(new IllegalArgumentException("Esse cargo não pode ser atribuido."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "ADMIN", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Esse cargo não pode ser atribuido.");
        }

        @Test
        void devePropagarExcecaoQuandoPapelNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 10, "INEXISTENTE"))
                    .thenThrow(new IllegalArgumentException("Papel não existe."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "INEXISTENTE", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Papel não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 999, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(999, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Discente semPermissao = discente();
            logarComo(semPermissao);
            when(grupoService.atribuirCargo(semPermissao, 42, 10, "TESOUREIRO"))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não possui permissão.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoEstaAprovado() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 10, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Grupo precisa estar ativo/aprovado."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo precisa estar ativo/aprovado.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 999, 10, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 999, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 10, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é discente.");
        }

        @Test
        void devePropagarCargoEmMinusculoSemNormalizarNoController() {
            // O controller não normaliza (toUpperCase) o parâmetro de cargo; quem faz
            // isso é o service. Garantimos que a string chega intacta.
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.atribuirCargo(responsavel, 42, 10, "tesoureiro"))
                    .thenThrow(new IllegalArgumentException("Papel não existe."));

            assertThatThrownBy(() -> grupoController.atribuirCargo(10, 42, "tesoureiro", session))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(grupoService, times(1)).atribuirCargo(responsavel, 42, 10, "tesoureiro");
        }
    }

    // removerCargo (PATCH /api/usuario/removercargo/{idGrupo}/{idDiscente}) --

    @Nested
    class RemoverCargo {

        @Test
        void devePermitirRemocaoQuandoSolicitanteTemPermissao() {
            Docente responsavel = docente();
            logarComo(responsavel);
            Grupo esperado = grupo(Status.APROVADO, responsavel);
            // Ordem real esperada pelo service: (solicitante, idDiscente, idGrupo, cargo)
            when(grupoService.removerCargo(responsavel, 42, 10, "TESOUREIRO")).thenReturn(esperado);

            Grupo resultado = grupoController.removerCargo(10, 42, "TESOUREIRO", session);

            assertThat(resultado).isEqualTo(esperado);
            verify(grupoService, times(1)).removerCargo(responsavel, 42, 10, "TESOUREIRO");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEstaLogado() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn(7);
            when(usuarioService.buscarPorId(7)).thenReturn(null);

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitante não foi encontrado.");

            verify(grupoService, never()).removerCargo(any(), any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoSessaoNaoTemAtributo() {
            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");

            verify(grupoService, never()).removerCargo(any(), any(), any(), any());
        }

        @Test
        void deveLancarExcecaoQuandoAtributoDeSessaoTemTipoInvalido() {
            when(session.getAttribute("IdUsuarioLogado")).thenReturn("nao-e-um-id");

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(SecurityException.class)
                    .isNotInstanceOf(ClassCastException.class)
                    .hasMessageContaining("É preciso estar logado para chamar esse método.");
        }

        @Test
        void devePropagarExcecaoQuandoCargoEhNulo() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 42, 10, null))
                    .thenThrow(new IllegalArgumentException("Cargo inválido."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, null, session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido.");
        }

        @Test
        void devePropagarExcecaoQuandoCargoEhEmBranco() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 42, 10, "   "))
                    .thenThrow(new IllegalArgumentException("Cargo inválido."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "   ", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido.");
        }

        @Test
        void devePropagarExcecaoQuandoPapelNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 42, 10, "INEXISTENTE"))
                    .thenThrow(new IllegalArgumentException("Papel não existe."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "INEXISTENTE", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Papel não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 42, 999, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.removerCargo(999, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoSolicitanteNaoTemPermissao() {
            Discente semPermissao = discente();
            logarComo(semPermissao);
            when(grupoService.removerCargo(semPermissao, 42, 10, "TESOUREIRO"))
                    .thenThrow(new SecurityException("Usuário não possui permissão."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Usuário não possui permissão.");
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoEstaAprovado() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 42, 10, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Grupo precisa estar ativo/aprovado."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo precisa estar ativo/aprovado.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoExiste() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 999, 10, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Usuário não existe."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 999, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 42, 10, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Usuário não é discente."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não é discente.");
        }

        @Test
        void devePropagarExcecaoQuandoDiscenteNaoPossuiCargo() {
            Docente responsavel = docente();
            logarComo(responsavel);
            when(grupoService.removerCargo(responsavel, 42, 10, "TESOUREIRO"))
                    .thenThrow(new IllegalArgumentException("Discente não possui esse cargo."));

            assertThatThrownBy(() -> grupoController.removerCargo(10, 42, "TESOUREIRO", session))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Discente não possui esse cargo.");
        }
    }

    // buscaId (GET /api/usuario/id/{id}) --------------------------------

    @Nested
    class BuscaId {

        @Test
        void deveRetornarGrupoQuandoEncontrado() {
            Grupo esperado = grupo(Status.APROVADO, docente());
            when(grupoService.buscaPorId(esperado.getId())).thenReturn(esperado);

            Grupo resultado = grupoController.buscaId(esperado.getId());

            assertThat(resultado).isEqualTo(esperado);
        }

        @Test
        void deveRetornarNuloQuandoGrupoNaoEncontrado() {
            when(grupoService.buscaPorId(999)).thenReturn(null);

            Grupo resultado = grupoController.buscaId(999);

            assertThat(resultado).isNull();
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            when(grupoService.buscaPorId(null))
                    .thenThrow(new IllegalArgumentException("ID inválido."));

            assertThatThrownBy(() -> grupoController.buscaId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido.");
        }
    }

    // lista (GET /api/usuario/lista) -------------------------------------

    @Nested
    class Lista {

        @Test
        void deveRetornarTodosOsGrupos() {
            Grupo grupoA = grupo(Status.APROVADO, docente());
            Grupo grupoB = grupo(Status.PENDENTE, docente());
            when(grupoService.listaGrupos()).thenReturn(List.of(grupoA, grupoB));

            List<Grupo> resultado = grupoController.lista();

            assertThat(resultado).containsExactly(grupoA, grupoB);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaGrupos() {
            when(grupoService.listaGrupos()).thenReturn(List.of());

            List<Grupo> resultado = grupoController.lista();

            assertThat(resultado).isEmpty();
        }
    }

    // listaMembros (GET /api/usuario/lista/membros/{id}) -----------------

    @Nested
    class ListaMembros {

        @Test
        void deveRetornarMembrosDoGrupo() {
            Discente membro1 = discente();
            Discente membro2 = discente();
            when(grupoService.listaGrupoMembros(10)).thenReturn(List.of(membro1, membro2));

            List<Discente> resultado = grupoController.listaMembros(10);

            assertThat(resultado).containsExactly(membro1, membro2);
        }

        @Test
        void deveRetornarListaVaziaQuandoGrupoNaoTemMembros() {
            when(grupoService.listaGrupoMembros(10)).thenReturn(List.of());

            List<Discente> resultado = grupoController.listaMembros(10);

            assertThat(resultado).isEmpty();
        }

        @Test
        void devePropagarExcecaoQuandoGrupoNaoExiste() {
            when(grupoService.listaGrupoMembros(999))
                    .thenThrow(new IllegalArgumentException("Grupo não existe."));

            assertThatThrownBy(() -> grupoController.listaMembros(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe.");
        }

        @Test
        void devePropagarExcecaoQuandoIdEhNulo() {
            when(grupoService.listaGrupoMembros(null))
                    .thenThrow(new IllegalArgumentException("ID inválido."));

            assertThatThrownBy(() -> grupoController.listaMembros(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido.");
        }
    }
}
