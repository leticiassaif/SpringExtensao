package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.SolicitacaoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Docente;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.model.Solicitacao;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.SolicitacaoRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitacaoServiceTest {

    @Mock
    SolicitacaoRepo solicitacaoRepo;

    @Mock
    PapelRepo papelRepo;

    @Mock
    UsuarioRepo usuarioRepo;

    @Mock
    UsuarioService usuarioService;

    @InjectMocks
    SolicitacaoService solicitacaoService;

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

    private Discente discente(Integer cargaHoraria) {
        Discente d = Discente.builder()
                .nome("Joana")
                .email("joana@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>())
                .cargaHoraria(cargaHoraria)
                .build();
        d.setId(nextId++);
        return d;
    }

    private Solicitacao solicitacao(Status status, Discente discente, Integer cargaHorario, LocalDate prazoReenvio) {
        Solicitacao s = Solicitacao.builder()
                .descricao("Monitoria de Cálculo I")
                .discente(discente)
                .cargaHorario(cargaHorario)
                .dataSolicitacao(LocalDate.now())
                .dataAtual(LocalDate.now())
                .status(status)
                .prazoReenvio(prazoReenvio)
                .build();
        s.setId(nextId++);
        return s;
    }

    // submeter --------------------------------------------------------------

    @Nested
    class Submeter {

        @Test
        void deveSubmeterQuandoDiscenteExiste() {
            Discente discente = discente(0);
            SolicitacaoDTO dto = SolicitacaoDTO.builder()
                    .descricao("Monitoria")
                    .cargaHoraria(20)
                    .dataSolicitacao(LocalDate.now().toString())
                    .idDiscente(discente.getId())
                    .build();

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.submeter(dto);

            assertThat(resultado.getDescricao()).isEqualTo("Monitoria");
            assertThat(resultado.getDiscente()).isEqualTo(discente);
            assertThat(resultado.getCargaHorario()).isEqualTo(20);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            SolicitacaoDTO dto = SolicitacaoDTO.builder()
                    .idDiscente(999)
                    .dataSolicitacao(LocalDate.now().toString())
                    .build();

            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> solicitacaoService.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não existe");

            verify(solicitacaoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            SolicitacaoDTO dto = SolicitacaoDTO.builder()
                    .idDiscente(docente.getId())
                    .dataSolicitacao(LocalDate.now().toString())
                    .build();

            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);

            assertThatThrownBy(() -> solicitacaoService.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        // --- testes de quebra ---

        @Test
        void deveLancarExcecaoQuandoDataSolicitacaoEhTextoInvalido() {
            Discente discente = discente(0);
            SolicitacaoDTO dto = SolicitacaoDTO.builder()
                    .idDiscente(discente.getId())
                    .dataSolicitacao("31/02/2025") // não é ISO-8601, formato errado
                    .build();

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);

            assertThatThrownBy(() -> solicitacaoService.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Data de solicitação inválida");
        }

        @Test
        void deveLancarExcecaoControladaQuandoDataSolicitacaoEhNula() {
            // BUG CONHECIDO: submeter() hoje lança NullPointerException quando
            // dataSolicitacao é null (LocalDate.parse(null)). O esperado é uma
            // exceção controlada de validação. Este teste fica VERMELHO até a
            // validação ser adicionada.
            Discente discente = discente(0);
            SolicitacaoDTO dto = SolicitacaoDTO.builder()
                    .idDiscente(discente.getId())
                    .dataSolicitacao(null)
                    .build();

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);

            assertThatThrownBy(() -> solicitacaoService.submeter(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }

        @Test
        void deveDefinirStatusPendenteAoSubmeter() {
            // BUG CONHECIDO: submeter() atualmente não seta status nenhum,
            // a solicitação nasce com status == null em vez de PENDENTE.
            // Este teste fica VERMELHO até o service ser corrigido.
            Discente discente = discente(0);
            SolicitacaoDTO dto = SolicitacaoDTO.builder()
                    .idDiscente(discente.getId())
                    .dataSolicitacao(LocalDate.now().toString())
                    .build();

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.submeter(dto);

            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
        }

        @Test
        void deveCriarSolicitacaoComCargaHorariaNulaNoDto() {
            Discente discente = discente(0);
            SolicitacaoDTO dto = SolicitacaoDTO.builder()
                    .idDiscente(discente.getId())
                    .dataSolicitacao(LocalDate.now().toString())
                    .cargaHoraria(null)
                    .build();

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.submeter(dto);

            // cargaHoraria null no DTO é armazenada sem quebrar; validação deve ocorrer em aprovar()
            assertThat(resultado.getCargaHorario()).isNull();
        }
    }

    // aprovar --------------------------------------------------------------

    @Nested
    class Aprovar {

        @Test
        void deveAprovarQuandoAdmin() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docente(admin);
            Discente discente = discente(10);
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente, 5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            solicitacaoService.aprovar(solicitante, solicitacao.getId());

            assertThat(solicitacao.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(discente.getCargaHoraria()).isEqualTo(15);
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Discente solicitante = discente(0);
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente(0), 5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));

            assertThatThrownBy(() -> solicitacaoService.aprovar(solicitante, solicitacao.getId()))
                    .isInstanceOf(SecurityException.class);

            verify(solicitacaoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
            Docente admin = docente(papel("ADMIN"));
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin.getCargos().get(0));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitacaoService.aprovar(admin, 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoEstaPendente() {
            Docente admin = docente(papel("ADMIN"));
            Solicitacao solicitacao = solicitacao(Status.APROVADO, discente(0), 5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin.getCargos().get(0));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThatThrownBy(() -> solicitacaoService.aprovar(admin, solicitacao.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }

        // --- testes de quebra ---

        @Test
        void deveLancarExcecaoQuandoSolicitanteEhNulo() {
            // hasPermissao(null, papel) lança IllegalArgumentException, não SecurityException
            assertThatThrownBy(() -> solicitacaoService.aprovar(null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário inválido");
        }

        @Test
        void deveTratarCargaHorariaNulaDoDiscenteComoZeroAoAprovar() {
            // BUG CONHECIDO: aprovar() hoje lança NullPointerException quando
            // discente.getCargaHoraria() é null. O esperado é tratar como 0
            // em vez de quebrar. Este teste fica VERMELHO até a correção.
            Docente admin = docente(papel("ADMIN"));
            Discente discenteSemCarga = discente(null);
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discenteSemCarga, 5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin.getCargos().get(0));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            solicitacaoService.aprovar(admin, solicitacao.getId());

            assertThat(discenteSemCarga.getCargaHoraria()).isEqualTo(5);
        }

        @Test
        void deveLancarExcecaoQuandoCargaHorariaDaSolicitacaoEhNegativa() {
            // BUG CONHECIDO: aprovar() hoje aceita cargaHorario negativo e
            // subtrai silenciosamente da carga horária do discente. O esperado
            // é rejeitar. Este teste fica VERMELHO até a validação ser adicionada.
            Docente admin = docente(papel("ADMIN"));
            Discente discente = discente(10);
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente, -50, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin.getCargos().get(0));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThatThrownBy(() -> solicitacaoService.aprovar(admin, solicitacao.getId()))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(solicitacaoRepo, never()).save(any());
        }

        // BUG CONHECIDO: aprovar() seta status = APROVADO ANTES de validar cargaHorario;
        // com carga negativa a exceção é lançada mas o objeto em memória já tem status
        // APROVADO (estado inconsistente). Fica VERMELHO até a reordenação da validação.
        @Test
        void naoDeveAltararStatusEmMemoriaQuandoCargaNegativaEhRejeitada() {
            Papel admin = papel("ADMIN");
            Docente solicitanteAdmin = docente(admin);
            Discente discente = discente(10);
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente, -5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThatThrownBy(() -> solicitacaoService.aprovar(solicitanteAdmin, solicitacao.getId()))
                    .isInstanceOf(IllegalArgumentException.class);

            // status não deve ter mudado — exceção ocorreu após setStatus(APROVADO), bug de ordem
            assertThat(solicitacao.getStatus()).isEqualTo(Status.PENDENTE);
        }

        @Test
        void deveAprovarComCargaHorariaZero() {
            Papel admin = papel("ADMIN");
            Docente solicitanteAdmin = docente(admin);
            Discente discente = discente(10);
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente, 0, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            solicitacaoService.aprovar(solicitanteAdmin, solicitacao.getId());

            assertThat(solicitacao.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(discente.getCargaHoraria()).isEqualTo(10); // 10 + 0
        }
    }

    // indeferir --------------------------------------------------------------

    @Nested
    class Indeferir {

        @Test
        void deveIndeferirQuandoParecerValido() {
            Docente coordenador = docente(papel("COORDENADOR"));
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente(0), 5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador.getCargos().get(0));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.indeferir(coordenador, solicitacao.getId(), "Carga horária insuficiente");

            assertThat(resultado.getStatus()).isEqualTo(Status.INDEFERIDO);
            assertThat(resultado.getParecer()).isEqualTo("Carga horária insuficiente");
            assertThat(resultado.getPrazoReenvio()).isEqualTo(LocalDate.now().plusDays(5));
        }

        @Test
        void deveLancarExcecaoQuandoParecerNulo() {
            assertThatThrownBy(() -> solicitacaoService.indeferir(docente(papel("ADMIN")), 1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parecer inválido");

            verifyNoInteractions(solicitacaoRepo);
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Discente solicitante = discente(0);

            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));

            assertThatThrownBy(() -> solicitacaoService.indeferir(solicitante, 1, "motivo"))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoEstaPendente() {
            Docente admin = docente(papel("ADMIN"));
            Solicitacao solicitacao = solicitacao(Status.CANCELADO, discente(0), 5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin.getCargos().get(0));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThatThrownBy(() -> solicitacaoService.indeferir(admin, solicitacao.getId(), "motivo"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitacaoService.indeferir(docente(admin), 999, "motivo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        // --- teste de quebra ---

        @Test
        void deveLancarExcecaoQuandoParecerEhEmBranco() {
            // BUG CONHECIDO: indeferir() hoje só valida parecer == null, então
            // uma string vazia/só-espaços passa direto (inconsistente com
            // GrupoService.rejeitar, que valida isBlank()). O esperado é rejeitar.
            // Este teste fica VERMELHO até a validação ser adicionada.
            Docente admin = docente(papel("ADMIN"));
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente(0), 5, null);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin.getCargos().get(0));
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(papel("COORDENADOR"));
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThatThrownBy(() -> solicitacaoService.indeferir(admin, solicitacao.getId(), "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parecer inválido");

            verify(solicitacaoRepo, never()).save(any());
        }
    }

    // reenviar --------------------------------------------------------------

    @Nested
    class Reenviar {

        @Test
        void deveReenviarQuandoDentroDoPrazo() {
            Solicitacao solicitacao = solicitacao(Status.INDEFERIDO, discente(0), 5, LocalDate.now().plusDays(2));
            solicitacao.setParecer("motivo anterior");

            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.reenviar(solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
            assertThat(resultado.getParecer()).isNull();
            assertThat(resultado.getPrazoReenvio()).isNull();
        }

        @Test
        void deveCancelarQuandoPrazoExpirado() {
            Solicitacao solicitacao = solicitacao(Status.INDEFERIDO, discente(0), 5, LocalDate.now().minusDays(1));

            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.reenviar(solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.CANCELADO);
            assertThat(resultado.getParecer()).isEqualTo("O Aluno não fez o reenvio a tempo");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitacaoNaoExiste() {
            when(solicitacaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> solicitacaoService.reenviar(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitação não existe");
        }

        @Test
        void deveLancarExcecaoQuandoStatusNaoEhIndeferido() {
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente(0), 5, null);

            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThatThrownBy(() -> solicitacaoService.reenviar(solicitacao.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não foi indeferida");
        }

        // --- testes de quebra ---

        @Test
        void deveLancarExcecaoControladaQuandoPrazoReenvioEhNulo() {
            // BUG CONHECIDO: reenviar() hoje lança NullPointerException quando
            // prazoReenvio é null (ex: status INDEFERIDO setado fora do fluxo
            // normal). O esperado é uma exceção controlada, não NPE.
            // Este teste fica VERMELHO até a defesa ser adicionada.
            Solicitacao solicitacao = solicitacao(Status.INDEFERIDO, discente(0), 5, null);

            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThatThrownBy(() -> solicitacaoService.reenviar(solicitacao.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }

        @Test
        void deveTratarPrazoNoLimiteExatoComoNaoExpirado() {
            // isBefore(hoje) é estritamente "antes" -> prazo == hoje conta como dentro do prazo
            Solicitacao solicitacao = solicitacao(Status.INDEFERIDO, discente(0), 5, LocalDate.now());
            solicitacao.setParecer("motivo anterior");

            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.reenviar(solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
        }

        @Test
        void deveLimparPrazoReenvioAoCancelar() {
            Solicitacao solicitacao = solicitacao(Status.INDEFERIDO, discente(0), 5, LocalDate.now().minusDays(1));

            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));
            when(solicitacaoRepo.save(any(Solicitacao.class))).thenAnswer(inv -> inv.getArgument(0));

            Solicitacao resultado = solicitacaoService.reenviar(solicitacao.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.CANCELADO);
            assertThat(resultado.getPrazoReenvio()).isNull();
        }
    }

    // buscarPorId --------------------------------------------------------------

    @Nested
    class BuscarPorId {

        @Test
        void deveRetornarSolicitacaoQuandoExiste() {
            Solicitacao solicitacao = solicitacao(Status.PENDENTE, discente(0), 5, null);
            when(solicitacaoRepo.findById(solicitacao.getId())).thenReturn(Optional.of(solicitacao));

            assertThat(solicitacaoService.buscarPorId(solicitacao.getId())).isEqualTo(solicitacao);
        }

        @Test
        void deveRetornarNuloQuandoNaoEncontrada() {
            when(solicitacaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThat(solicitacaoService.buscarPorId(999)).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoIdNulo() {
            assertThatThrownBy(() -> solicitacaoService.buscarPorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");
        }
    }

    // listarPorDiscente --------------------------------------------------------------

    @Nested
    class ListarPorDiscente {

        @Test
        void deveListarQuandoDiscenteExiste() {
            Discente discente = discente(0);
            List<Solicitacao> esperado = List.of(solicitacao(Status.PENDENTE, discente, 5, null));

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(solicitacaoRepo.findByDiscente(discente)).thenReturn(esperado);

            assertThat(solicitacaoService.listarPorDiscente(discente.getId())).isEqualTo(esperado);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> solicitacaoService.listarPorDiscente(999))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);

            assertThatThrownBy(() -> solicitacaoService.listarPorDiscente(docente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void deveRetornarListaVaziaQuandoDiscenteNaoTemSolicitacoes() {
            Discente discente = discente(0);
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(solicitacaoRepo.findByDiscente(discente)).thenReturn(List.of());

            assertThat(solicitacaoService.listarPorDiscente(discente.getId())).isEmpty();
        }
    }

    // listarIndeferidos --------------------------------------------------------------

    @Nested
    class ListarIndeferidos {

        @Test
        void deveListarIndeferidosQuandoDiscenteExiste() {
            Discente discente = discente(0);
            List<Solicitacao> esperado = List.of(solicitacao(Status.INDEFERIDO, discente, 5, LocalDate.now().plusDays(5)));

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(solicitacaoRepo.findByDiscenteAndStatus(discente, Status.INDEFERIDO)).thenReturn(esperado);

            assertThat(solicitacaoService.listarIndeferidos(discente.getId())).isEqualTo(esperado);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> solicitacaoService.listarIndeferidos(999))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);

            assertThatThrownBy(() -> solicitacaoService.listarIndeferidos(docente.getId()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // listarPendentes --------------------------------------------------------------

    @Nested
    class ListarPendentes {

        @Test
        void deveListarTodasAsPendentes() {
            List<Solicitacao> esperado = List.of(solicitacao(Status.PENDENTE, discente(0), 5, null));
            when(solicitacaoRepo.findByStatus(Status.PENDENTE)).thenReturn(esperado);

            assertThat(solicitacaoService.listarPendentes()).isEqualTo(esperado);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaPendentes() {
            when(solicitacaoRepo.findByStatus(Status.PENDENTE)).thenReturn(List.of());

            assertThat(solicitacaoService.listarPendentes()).isEmpty();
        }
    }
}