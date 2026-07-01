package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.InscricaoRepo;
import br.ufma.springextensao.repository.OportunidadeRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscricaoServiceTest {

    @Mock
    InscricaoRepo inscricaoRepo;

    @Mock
    UsuarioService usuarioService;

    @Mock
    OportunidadeService oportunidadeService;

    @Mock
    OportunidadeRepo oportunidadeRepo;

    @Mock
    UsuarioRepo usuarioRepo;

    @InjectMocks
    InscricaoService inscricaoService;

    // Helpers ----------------------------------------------------------

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
                .oportunidades(new ArrayList<>())
                .build();
        d.setId(nextId++);
        return d;
    }

    private Oportunidade oportunidade(StatusOp status, Docente coordenador, Integer vagasLivres) {
        Oportunidade o = Oportunidade.builder()
                .titulo("Monitoria de Cálculo I")
                .cargaHoraria(60)
                .status(status)
                .coordenador(coordenador)
                .vagasLivres(vagasLivres)
                .discentesOp(new ArrayList<>())
                .build();
        o.setId(nextId++);
        return o;
    }

    private Inscricao inscricao(Status status, Discente discente, Oportunidade oportunidade) {
        Inscricao i = Inscricao.builder()
                .motivacao("Interesse na área")
                .status(status)
                .discente(discente)
                .oportunidade(oportunidade)
                .build();
        i.setId(nextId++);
        return i;
    }

    private InscricaoDTO inscricaoDTO(Integer idOportunidade, Integer idDiscente, String motivacao) {
        InscricaoDTO dto = new InscricaoDTO();
        dto.setIdOportunidade(idOportunidade);
        dto.setIdDiscente(idDiscente);
        dto.setMotivacao(motivacao);
        return dto;
    }

    // inscrever -------------------------------------------------------------

    @Nested
    class Inscrever {

        @Test
        void devePermitirInscricaoComDadosValidos() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            when(inscricaoRepo.findByDiscenteAndOportunidade(discente, oportunidade)).thenReturn(Optional.empty());
            when(inscricaoRepo.save(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));
            InscricaoDTO dto = inscricaoDTO(oportunidade.getId(), discente.getId(), "Interesse na área");

            Inscricao resultado = inscricaoService.inscrever(dto);

            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
            assertThat(resultado.getDiscente()).isEqualTo(discente);
            assertThat(resultado.getOportunidade()).isEqualTo(oportunidade);
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            Discente discente = discente();
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(oportunidadeService.buscarOportunidadePorId(999)).thenReturn(null);
            InscricaoDTO dto = inscricaoDTO(999, discente.getId(), "Interesse");

            assertThatThrownBy(() -> inscricaoService.inscrever(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não existente");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            when(usuarioService.buscarPorId(999)).thenReturn(null);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            InscricaoDTO dto = inscricaoDTO(oportunidade.getId(), 999, "Interesse");

            assertThatThrownBy(() -> inscricaoService.inscrever(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existente");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            InscricaoDTO dto = inscricaoDTO(oportunidade.getId(), docente.getId(), "Interesse");

            assertThatThrownBy(() -> inscricaoService.inscrever(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("precisa ser discente");
        }

        @Test
        void deveLancarExcecaoQuandoMotivacaoEmBranco() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            InscricaoDTO dto = inscricaoDTO(oportunidade.getId(), discente.getId(), "   ");

            assertThatThrownBy(() -> inscricaoService.inscrever(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Motivação ausentes.");
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoEstaAberta() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ENCERRADA, docente(), 2);
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            InscricaoDTO dto = inscricaoDTO(oportunidade.getId(), discente.getId(), "Interesse");

            assertThatThrownBy(() -> inscricaoService.inscrever(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está aberta");
        }

        @Test
        void deveLancarExcecaoQuandoDiscenteJaEstaInscrito() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            when(inscricaoRepo.findByDiscenteAndOportunidade(discente, oportunidade))
                    .thenReturn(Optional.of(inscricao(Status.PENDENTE, discente, oportunidade)));
            InscricaoDTO dto = inscricaoDTO(oportunidade.getId(), discente.getId(), "Interesse");

            assertThatThrownBy(() -> inscricaoService.inscrever(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já foi inscrito");

            verify(inscricaoRepo, never()).save(any());
        }
    }

    // aprovar -----------------------------------------------------------------

    @Nested
    class Aprovar {

        @Test
        void devePermitirAprovacaoQuandoHaVagas() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente, oportunidade);
            Docente solicitante = docente();
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
            when(inscricaoRepo.save(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

            Inscricao resultado = inscricaoService.aprovar(inscricao.getId(), solicitante);

            assertThat(resultado.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(oportunidade.getDiscentesOp()).contains(discente);
            assertThat(discente.getOportunidades()).contains(oportunidade);
            verify(oportunidadeService).diminuiVagasLivres(oportunidade);
            verify(usuarioRepo).save(discente);
            verify(oportunidadeRepo).save(oportunidade);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteEhNulo() {
            assertThatThrownBy(() -> inscricaoService.aprovar(1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O solicitante deve ser informado");

            verifyNoInteractions(inscricaoRepo);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhDocente() {
            Discente solicitante = discente();

            assertThatThrownBy(() -> inscricaoService.aprovar(1, solicitante))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("precisa ser docente");
        }

        @Test
        void deveLancarExcecaoQuandoInscricaoNaoExiste() {
            when(inscricaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inscricaoService.aprovar(999, docente()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Inscrição não existe");
        }

        @Test
        void deveLancarExcecaoQuandoInscricaoNaoEstaPendente() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            Inscricao inscricao = inscricao(Status.APROVADO, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.aprovar(inscricao.getId(), docente()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Só é possível aprovar inscrições pendentes.");
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeSemVagasLivres() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 0);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.aprovar(inscricao.getId(), docente()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não possui vagas livres");

            verify(inscricaoRepo, never()).save(any());
        }
    }

    // rejeitar ------------------------------------------------------------

    @Nested
    class Rejeitar {

        @Test
        void devePermitirRejeicaoPeloResponsavel() {
            Docente responsavel = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
            when(inscricaoRepo.save(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

            Inscricao resultado = inscricaoService.rejeitar(inscricao.getId(), "Fora do perfil", responsavel);

            assertThat(resultado.getStatus()).isEqualTo(Status.REJEITADO);
            assertThat(resultado.getJustificativaCancelamento()).isEqualTo("Fora do perfil");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteInativo() {
            Docente solicitante = docente();
            solicitante.setAtivo(false);

            assertThatThrownBy(() -> inscricaoService.rejeitar(1, "motivo", solicitante))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("O solicitante deve ser informado");

            verifyNoInteractions(inscricaoRepo);
        }

        @Test
        void deveLancarExcecaoQuandoInscricaoNaoExiste() {
            when(inscricaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inscricaoService.rejeitar(999, "motivo", docente()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Inscrição não existe.");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhOResponsavel() {
            Docente responsavel = docente();
            Docente outroDocente = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.rejeitar(inscricao.getId(), "motivo", outroDocente))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Apenas o responsável pode fazer isso.");
        }

        @Test
        void deveLancarExcecaoQuandoJustificativaEmBranco() {
            Docente responsavel = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.rejeitar(inscricao.getId(), "   ", responsavel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("A justificativa não foi informada");
        }

        @Test
        void deveLancarExcecaoQuandoInscricaoNaoEstaPendente() {
            Docente responsavel = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.APROVADO, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.rejeitar(inscricao.getId(), "motivo", responsavel))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDENTES");
        }
    }

    // removerDiscente -----------------------------------------------------

    @Nested
    class RemoverDiscente {

        @Test
        void devePermitirRemocaoPeloResponsavelQuandoPendente() {
            // A checagem de status em removerDiscente exige PENDENTE, embora a
            // mensagem de erro fale em "inscrições aprovadas" — comportamento real do service.
            Docente responsavel = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
            when(inscricaoRepo.save(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

            Inscricao resultado = inscricaoService.removerDiscente(inscricao.getId(), "Descumpriu normas", responsavel);

            assertThat(resultado.getStatus()).isEqualTo(Status.CANCELADO);
            assertThat(resultado.getJustificativaCancelamento()).isEqualTo("Descumpriu normas");
            verify(oportunidadeService).aumentarVagasLivres(oportunidade);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhOResponsavel() {
            Docente responsavel = docente();
            Docente outroDocente = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.removerDiscente(inscricao.getId(), "motivo", outroDocente))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Apenas o responsável pode fazer isso.");
        }

        @Test
        void deveLancarExcecaoQuandoJustificativaNula() {
            Docente responsavel = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.removerDiscente(inscricao.getId(), null, responsavel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("A justificativa não foi informada");
        }

        @Test
        void deveLancarExcecaoQuandoInscricaoNaoEstaPendente() {
            Docente responsavel = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, responsavel, 2);
            Inscricao inscricao = inscricao(Status.APROVADO, discente(), oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.removerDiscente(inscricao.getId(), "motivo", responsavel))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Só é possível retirar inscrições aprovadas.");
        }
    }

    // desistir --------------------------------------------------------------

    @Nested
    class Desistir {

        @Test
        void devePermitirDesistenciaDeInscricaoAprovadaELiberarVaga() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 1);
            oportunidade.getDiscentesOp().add(discente);
            discente.getOportunidades().add(oportunidade);
            Inscricao inscricao = inscricao(Status.APROVADO, discente, oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            Inscricao resultado = inscricaoService.desistir(inscricao.getId(), discente);

            assertThat(resultado.getStatus()).isEqualTo(Status.CANCELADO);
            assertThat(resultado.getJustificativaCancelamento()).isEqualTo("O Discente desistiu da vaga");
            assertThat(oportunidade.getDiscentesOp()).doesNotContain(discente);
            assertThat(discente.getOportunidades()).doesNotContain(oportunidade);
            verify(oportunidadeService).aumentarVagasLivres(oportunidade);
        }

        @Test
        void devePermitirDesistenciaDeInscricaoPendenteSemMexerEmVagas() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 1);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente, oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            Inscricao resultado = inscricaoService.desistir(inscricao.getId(), discente);

            assertThat(resultado.getStatus()).isEqualTo(Status.CANCELADO);
            verifyNoInteractions(oportunidadeService);
        }

        @Test
        void deveLancarExcecaoQuandoInscricaoNaoExiste() {
            when(inscricaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inscricaoService.desistir(999, discente()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Inscrição não existe.");
        }

        @Test
        void deveLancarExcecaoQuandoInscricaoJaCancelada() {
            Discente discente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 1);
            Inscricao inscricao = inscricao(Status.CANCELADO, discente, oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.desistir(inscricao.getId(), discente))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já está rejeitada ou cancelada");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhODiscenteDaInscricao() {
            Discente discenteDaInscricao = discente();
            Discente outroDiscente = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 1);
            Inscricao inscricao = inscricao(Status.PENDENTE, discenteDaInscricao, oportunidade);
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThatThrownBy(() -> inscricaoService.desistir(inscricao.getId(), outroDiscente))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Apenas o próprio discente pode desistir");
        }
    }

    // listarPorOportunidade -------------------------------------------------

    @Nested
    class ListarPorOportunidade {

        @Test
        void deveListarInscricoesDaOportunidade() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            when(inscricaoRepo.findByOportunidade(oportunidade)).thenReturn(List.of(inscricao));

            assertThat(inscricaoService.listarPorOportunidade(oportunidade.getId())).containsExactly(inscricao);
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeService.buscarOportunidadePorId(999)).thenReturn(null);

            assertThatThrownBy(() -> inscricaoService.listarPorOportunidade(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não existe.");
        }
    }

    // listarFilaEspera ------------------------------------------------------

    @Nested
    class ListarFilaEspera {

        @Test
        void deveListarInscricoesPendentesDaOportunidade() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente(), 2);
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade);
            when(oportunidadeService.buscarOportunidadePorId(oportunidade.getId())).thenReturn(oportunidade);
            when(inscricaoRepo.findByOportunidadeAndStatus(oportunidade, Status.PENDENTE)).thenReturn(List.of(inscricao));

            assertThat(inscricaoService.listarFilaEspera(oportunidade.getId())).containsExactly(inscricao);
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeService.buscarOportunidadePorId(999)).thenReturn(null);

            assertThatThrownBy(() -> inscricaoService.listarFilaEspera(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não existe.");
        }
    }

    // listarPorDiscente -------------------------------------------------------

    @Nested
    class ListarPorDiscente {

        @Test
        void deveListarInscricoesDoDiscente() {
            Discente discente = discente();
            Inscricao inscricao = inscricao(Status.PENDENTE, discente, oportunidade(StatusOp.ABERTA, docente(), 2));
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(inscricaoRepo.findByDiscente(discente)).thenReturn(List.of(inscricao));

            assertThat(inscricaoService.listarPorDiscente(discente.getId())).containsExactly(inscricao);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> inscricaoService.listarPorDiscente(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente docente = docente();
            when(usuarioService.buscarPorId(docente.getId())).thenReturn(docente);

            assertThatThrownBy(() -> inscricaoService.listarPorDiscente(docente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("precisa ser discente");
        }
    }

    // buscaPorId --------------------------------------------------------------

    @Nested
    class BuscaPorId {

        @Test
        void deveRetornarInscricaoQuandoExiste() {
            Inscricao inscricao = inscricao(Status.PENDENTE, discente(), oportunidade(StatusOp.ABERTA, docente(), 2));
            when(inscricaoRepo.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

            assertThat(inscricaoService.buscaPorId(inscricao.getId())).isEqualTo(inscricao);
        }

        @Test
        void deveRetornarNuloQuandoNaoEncontrada() {
            when(inscricaoRepo.findById(999)).thenReturn(Optional.empty());

            assertThat(inscricaoService.buscaPorId(999)).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoIdNulo() {
            assertThatThrownBy(() -> inscricaoService.buscaPorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido.");
        }
    }
}
