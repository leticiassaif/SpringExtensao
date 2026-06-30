package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Docente;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Papel;
import br.ufma.springextensao.repository.OportunidadeRepo;
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
class OportunidadeServiceTest {

    @Mock
    OportunidadeRepo oportunidadeRepo;

    @InjectMocks
    OportunidadeService oportunidadeService;

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

    private Discente discente(Papel... cargos) {
        Discente d = Discente.builder()
                .nome("Joana")
                .email("joana@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>(List.of(cargos)))
                .build();
        d.setId(nextId++);
        return d;
    }

    private Oportunidade oportunidade(StatusOp status) {
        Oportunidade o = Oportunidade.builder()
                .titulo("Monitoria de Cálculo I")
                .descricao("Auxílio aos discentes da disciplina")
                .cargaHoraria(60)
                .vagas(2)
                .dataInicio(LocalDate.now().plusDays(10))
                .dataFim(LocalDate.now().plusDays(100))
                .status(status)
                .build();
        o.setId(nextId++);
        return o;
    }

    private OportunidadeDTO oportunidadeDTO(String titulo, String descricao, Integer cargaHoraria,
                                             Integer vagas, LocalDate dataInicio, LocalDate dataFim) {
        return OportunidadeDTO.builder()
                .titulo(titulo)
                .descricao(descricao)
                .cargaHoraria(cargaHoraria)
                .vagas(vagas)
                .dataInicio(dataInicio)
                .dataFim(dataFim)
                .build();
    }

    // buscarOportunidadePorId --------------------------------------------------------------

    @Nested
    class BuscarOportunidadePorId {

        @Test
        void deveRetornarOportunidadeQuandoExiste() {
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO);
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            Oportunidade resultado = oportunidadeService.buscarOportunidadePorId(oportunidade.getId());

            assertThat(resultado).isEqualTo(oportunidade);
        }

        @Test
        void deveLancarExcecaoQuandoNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.buscarOportunidadePorId(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            assertThatThrownBy(() -> oportunidadeService.buscarOportunidadePorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID é obrigatório");

            verifyNoInteractions(oportunidadeRepo);
        }
    }

    // criaOportunidade --------------------------------------------------------------

    @Nested
    class CriarOportunidade {

        @Test
        void deveCriarOportunidadeComDadosValidos() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria de Cálculo I", "Auxílio aos discentes", 60, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.criaOportunidade(dto);

            assertThat(resultado.getTitulo()).isEqualTo("Monitoria de Cálculo I");
            assertThat(resultado.getDescricao()).isEqualTo("Auxílio aos discentes");
            assertThat(resultado.getCargaHoraria()).isEqualTo(60);
            assertThat(resultado.getVagas()).isEqualTo(2);
        }

        @Test
        void deveDefinirStatusRascunhoAoCriar() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.criaOportunidade(dto);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.RASCUNHO);
        }

        @Test
        void deveLancarExcecaoQuandoTituloEhNulo() {
            OportunidadeDTO dto = oportunidadeDTO(null, "Descrição", 60, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Título é obrigatório");

            verify(oportunidadeRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoTituloEhEmBranco() {
            OportunidadeDTO dto = oportunidadeDTO("   ", "Descrição", 60, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Título é obrigatório");
        }

        @Test
        void deveLancarExcecaoQuandoDescricaoEhNula() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", null, 60, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Descrição é obrigatória");
        }

        @Test
        void deveLancarExcecaoQuandoDescricaoEhEmBranco() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "   ", 60, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Descrição é obrigatória");
        }

        @Test
        void deveLancarExcecaoQuandoCargaHorariaEhZero() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 0, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária deve ser positiva");
        }

        @Test
        void deveLancarExcecaoQuandoCargaHorariaEhNegativa() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", -10, 2,
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(100));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária deve ser positiva");
        }

        @Test
        void deveLancarExcecaoQuandoDataInicioEhNula() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, 2,
                    null, LocalDate.now().plusDays(100));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Datas de início e fim são obrigatórias");
        }

        @Test
        void deveLancarExcecaoQuandoDataFimEhNula() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, 2,
                    LocalDate.now().plusDays(10), null);

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Datas de início e fim são obrigatórias");
        }

        @Test
        void deveLancarExcecaoQuandoDataFimEhAntesDeDataInicio() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, 2,
                    LocalDate.now().plusDays(100), LocalDate.now().plusDays(10));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de fim não pode ser antes do início");
        }

        @Test
        void deveLancarExcecaoQuandoDataInicioEhNoPassado() {
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, 2,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de início não pode ser no passado");
        }

        // BUG CONHECIDO: criaOportunidade() lança NullPointerException quando o DTO é
        // null, pois oportunidade.getTitulo() é chamado sem validação prévia. O esperado
        // é uma exceção controlada. Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoControladaQuandoDtoEhNulo() {
            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }
    }

    // publicarOportunidade --------------------------------------------------------------

    @Nested
    class PublicarOportunidade {

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.publicarOportunidade(999, docente(papel("ADMIN"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }

        // BUG CONHECIDO: publicarOportunidade() não altera o status da oportunidade,
        // pois a regra de negócio que define o novo status (AGUARDA_APROVACAO para
        // discente diretor, ABERTA para docente) está comentada no service. O esperado
        // é que o status mude de RASCUNHO para AGUARDA_APROVACAO. Fica VERMELHO até a
        // correção.
        @Test
        void deveMudarStatusParaAguardaAprovacaoQuandoSolicitanteEhDiscenteDiretor() {
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO);
            Discente discenteDiretor = discente(papel("DISCENTE_DIRETOR"));

            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.publicarOportunidade(oportunidade.getId(), discenteDiretor);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.AGUARDA_APROVACAO);
        }

        // BUG CONHECIDO: mesmo motivo do teste acima — status deveria mudar para ABERTA
        // quando quem publica é docente. Fica VERMELHO até a correção.
        @Test
        void deveMudarStatusParaAbertaQuandoSolicitanteEhDocente() {
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO);
            Docente docente = docente(papel("DOCENTE"));

            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.publicarOportunidade(oportunidade.getId(), docente);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.ABERTA);
        }

        // BUG CONHECIDO: publicarOportunidade() não valida permissão do solicitante —
        // a checagem está comentada no service, então qualquer usuário, mesmo sem
        // cargo, consegue publicar. O esperado é lançar SecurityException. Fica
        // VERMELHO até a validação ser reativada.
        @Test
        void deveLancarExcecaoQuandoSolicitanteSemPermissao() {
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO);
            Discente semCargo = discente();

            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            assertThatThrownBy(() -> oportunidadeService.publicarOportunidade(oportunidade.getId(), semCargo))
                    .isInstanceOf(SecurityException.class);

            verify(oportunidadeRepo, never()).save(any());
        }
    }

    // aprovarOportunidade --------------------------------------------------------------

    @Nested
    class AprovarOportunidade {

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.aprovarOportunidade(999, docente(papel("ADMIN"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }

        // BUG CONHECIDO: aprovarOportunidade() sempre retorna null, pois toda a regra
        // de aprovação (checagem de permissão e mudança de status para ABERTA) está
        // comentada no service. O esperado é retornar a oportunidade com status ABERTA.
        // Fica VERMELHO até a correção.
        @Test
        void deveAprovarOportunidadeAguardandoAprovacao() {
            Oportunidade oportunidade = oportunidade(StatusOp.AGUARDA_APROVACAO);
            Docente admin = docente(papel("ADMIN"));

            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.aprovarOportunidade(oportunidade.getId(), admin);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatus()).isEqualTo(StatusOp.ABERTA);
        }
    }

    // iniciarOportunidade --------------------------------------------------------------

    @Nested
    class IniciarOportunidade {

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.iniciarOportunidade(999, docente(papel("ADMIN"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }

        // BUG CONHECIDO: iniciarOportunidade() sempre retorna null, pois a regra que
        // muda o status de ABERTA para EM_EXECUCAO está comentada no service. Fica
        // VERMELHO até a correção.
        @Test
        void deveIniciarOportunidadeAberta() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA);
            Docente admin = docente(papel("ADMIN"));

            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.iniciarOportunidade(oportunidade.getId(), admin);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatus()).isEqualTo(StatusOp.EM_EXECUCAO);
        }
    }

    // encerrarOportunidade --------------------------------------------------------------

    @Nested
    class EncerrarOportunidade {

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.encerrarOportunidade(999, docente(papel("ADMIN"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }

        // BUG CONHECIDO: encerrarOportunidade() sempre retorna null, pois a regra que
        // muda o status de EM_EXECUCAO para ENCERRADA está comentada no service. Fica
        // VERMELHO até a correção.
        @Test
        void deveEncerrarOportunidadeEmExecucao() {
            Oportunidade oportunidade = oportunidade(StatusOp.EM_EXECUCAO);
            Docente admin = docente(papel("ADMIN"));

            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.encerrarOportunidade(oportunidade.getId(), admin);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatus()).isEqualTo(StatusOp.ENCERRADA);
        }
    }

    // cancelarOportunidade --------------------------------------------------------------

    @Nested
    class CancelarOportunidade {

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.cancelarOportunidade(999, docente(papel("ADMIN"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }

        // BUG CONHECIDO: cancelarOportunidade() sempre retorna null, pois a regra que
        // muda o status para CANCELADA está comentada no service (o trecho nem chega a
        // compilar a checagem de permissão). Fica VERMELHO até a correção.
        @Test
        void deveCancelarOportunidade() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA);
            Docente admin = docente(papel("ADMIN"));

            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.cancelarOportunidade(oportunidade.getId(), admin);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatus()).isEqualTo(StatusOp.CANCELADA);
        }
    }

    // listarOportunidades --------------------------------------------------------------

    @Nested
    class ListarOportunidades {

        @Test
        void deveRetornarTodasAsOportunidades() {
            Oportunidade oportunidadeA = oportunidade(StatusOp.ABERTA);
            Oportunidade oportunidadeB = oportunidade(StatusOp.RASCUNHO);
            when(oportunidadeRepo.findAll()).thenReturn(List.of(oportunidadeA, oportunidadeB));

            List<Oportunidade> resultado = oportunidadeService.listarOportunidades();

            assertThat(resultado).containsExactly(oportunidadeA, oportunidadeB);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaOportunidades() {
            when(oportunidadeRepo.findAll()).thenReturn(List.of());

            List<Oportunidade> resultado = oportunidadeService.listarOportunidades();

            assertThat(resultado).isEmpty();
        }
    }
}
