package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.enums.StatusOp;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.OportunidadeRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.TipoRepo;
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
class OportunidadeServiceTest {

    @Mock
    OportunidadeRepo oportunidadeRepo;

    @Mock
    PapelRepo papelRepo;

    @Mock
    TipoRepo tipoRepo;

    @Mock
    UsuarioService usuarioService;

    @Mock
    UsuarioRepo usuarioRepo;

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

    private Tipo tipo(String nome) {
        Tipo t = new Tipo();
        t.setId(nextId++);
        t.setNome(nome);
        return t;
    }

    private Oportunidade oportunidade(StatusOp status, Docente coordenador) {
        Oportunidade o = Oportunidade.builder()
                .titulo("Monitoria de Cálculo I")
                .descricao("Auxílio aos discentes da disciplina")
                .cargaHoraria(60)
                .vagas(2)
                .vagasLivres(2)
                .status(status)
                .coordenador(coordenador)
                .discentesOp(new ArrayList<>())
                .build();
        o.setId(nextId++);
        return o;
    }

    private OportunidadeDTO oportunidadeDTO(String titulo, String descricao, Integer cargaHoraria,
                                            String tipo, Integer idDocente) {
        return OportunidadeDTO.builder()
                .titulo(titulo)
                .descricao(descricao)
                .cargaHoraria(cargaHoraria)
                .vagas(2)
                .tipo(tipo)
                .idDocente(idDocente)
                .build();
    }

    // buscarOportunidadePorId --------------------------------------------

    @Nested
    class BuscarOportunidadePorId {

        @Test
        void deveRetornarOportunidadeQuandoExiste() {
            Oportunidade esperada = oportunidade(StatusOp.ABERTA, docente());
            when(oportunidadeRepo.findById(esperada.getId())).thenReturn(Optional.of(esperada));

            assertThat(oportunidadeService.buscarOportunidadePorId(esperada.getId())).isEqualTo(esperada);
        }

        @Test
        void deveLancarExcecaoQuandoNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.buscarOportunidadePorId(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }
    }

    // criaOportunidade ----------------------------------------------------

    @Nested
    class CriaOportunidade {

        private Papel diretor;

        private void comPapelDiretor() {
            diretor = papel("DIRETOR");
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
        }

        @Test
        void devePermitirCriacaoQuandoSolicitanteEhDiretorECriaParaSiMesmo() {
            comPapelDiretor();
            Docente solicitante = docente(diretor);
            when(usuarioService.buscarPorId(solicitante.getId())).thenReturn(solicitante);
            when(tipoRepo.findByNome("MONITORIA")).thenReturn(Optional.of(tipo("MONITORIA")));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, "monitoria", solicitante.getId());

            Oportunidade resultado = oportunidadeService.criaOportunidade(solicitante, dto);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.RASCUNHO);
            assertThat(resultado.getCoordenador()).isEqualTo(solicitante);
            assertThat(resultado.getTitulo()).isEqualTo("Monitoria");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteSemPermissaoNemDocente() {
            comPapelDiretor();
            Discente solicitante = discente();
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, "MONITORIA", 1);

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");

            verify(oportunidadeRepo, never()).save(any());
        }

        @Test
        void devePropagarExcecaoQuandoDocenteSolicitanteNaoEhDiretor() {
            // Docente sem papel DIRETOR passa na primeira checagem (é Docente), mas
            // a segunda checagem em criaOportunidade exige hasPermissao(solicitante, DIRETOR),
            // então a criação só é permitida de fato para diretores.
            comPapelDiretor();
            Docente solicitante = docente();
            when(tipoRepo.findByNome("MONITORIA")).thenReturn(Optional.of(tipo("MONITORIA")));
            when(usuarioService.buscarPorId(solicitante.getId())).thenReturn(solicitante);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, "MONITORIA", solicitante.getId());

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não tem permissão.");
        }

        @Test
        void deveLancarExcecaoQuandoTituloEmBranco() {
            comPapelDiretor();
            Docente solicitante = docente(diretor);
            OportunidadeDTO dto = oportunidadeDTO("   ", "Descrição", 60, "MONITORIA", solicitante.getId());

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Título é obrigatório.");
        }

        @Test
        void deveLancarExcecaoQuandoDescricaoEmBranco() {
            comPapelDiretor();
            Docente solicitante = docente(diretor);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "   ", 60, "MONITORIA", solicitante.getId());

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Descrição é obrigatória.");
        }

        @Test
        void deveLancarExcecaoQuandoCargaHorariaNaoEhPositiva() {
            comPapelDiretor();
            Docente solicitante = docente(diretor);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 0, "MONITORIA", solicitante.getId());

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Carga horária deve ser positiva.");
        }

        @Test
        void deveLancarExcecaoQuandoTipoNaoExiste() {
            comPapelDiretor();
            Docente solicitante = docente(diretor);
            when(tipoRepo.findByNome("INEXISTENTE")).thenReturn(Optional.empty());
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, "inexistente", solicitante.getId());

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tipo não existe.");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioIndicadoNaoExiste() {
            comPapelDiretor();
            Docente solicitante = docente(diretor);
            when(tipoRepo.findByNome("MONITORIA")).thenReturn(Optional.of(tipo("MONITORIA")));
            when(usuarioService.buscarPorId(999)).thenReturn(null);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, "MONITORIA", 999);

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe.");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioIndicadoNaoEhDocente() {
            comPapelDiretor();
            Docente solicitante = docente(diretor);
            Discente naoDocente = discente();
            when(tipoRepo.findByNome("MONITORIA")).thenReturn(Optional.of(tipo("MONITORIA")));
            when(usuarioService.buscarPorId(naoDocente.getId())).thenReturn(naoDocente);
            OportunidadeDTO dto = oportunidadeDTO("Monitoria", "Descrição", 60, "MONITORIA", naoDocente.getId());

            assertThatThrownBy(() -> oportunidadeService.criaOportunidade(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não tem permissão.");
        }
    }

    // publicarOportunidade -------------------------------------------------

    @Nested
    class PublicarOportunidade {

        @Test
        void devePermitirPublicacaoQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO, solicitante);
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.publicarOportunidade(oportunidade.getId(), solicitante);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.ABERTA);
        }

        @Test
        void devePublicarComoAguardandoAprovacaoQuandoSolicitanteNaoEhDocente() {
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
            Discente solicitante = discente(diretor);
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO, docente());
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.publicarOportunidade(oportunidade.getId(), solicitante);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.AGUARDA_APROVACAO);
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            when(papelRepo.findByNome("DIRETOR")).thenReturn(papel("DIRETOR"));
            Discente solicitante = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO, docente());
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            assertThatThrownBy(() -> oportunidadeService.publicarOportunidade(oportunidade.getId(), solicitante))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("não possui permissão");

            verify(oportunidadeRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.publicarOportunidade(999, docente()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }
    }

    // aprovarOportunidade ---------------------------------------------------

    @Nested
    class AprovarOportunidade {

        @Test
        void devePermitirAprovacaoQuandoDocenteEhAdmin() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            Docente solicitante = docente(admin);
            Oportunidade oportunidade = oportunidade(StatusOp.AGUARDA_APROVACAO, docente());
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.aprovarOportunidade(oportunidade.getId(), solicitante);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.ABERTA);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhDocente() {
            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            Discente solicitante = discente();
            Oportunidade oportunidade = oportunidade(StatusOp.AGUARDA_APROVACAO, docente());
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            assertThatThrownBy(() -> oportunidadeService.aprovarOportunidade(oportunidade.getId(), solicitante))
                    .isInstanceOf(SecurityException.class);

            verify(oportunidadeRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoDocenteNaoEhAdmin() {
            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            Docente solicitante = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.AGUARDA_APROVACAO, docente());
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            assertThatThrownBy(() -> oportunidadeService.aprovarOportunidade(oportunidade.getId(), solicitante))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.aprovarOportunidade(999, docente(papel("ADMIN"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoEstaAguardandoAprovacao() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            Docente solicitante = docente(admin);
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente());
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            assertThatThrownBy(() -> oportunidadeService.aprovarOportunidade(oportunidade.getId(), solicitante))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("aguardando aprovação");
        }
    }

    // iniciarOportunidade -----------------------------------------------------

    @Nested
    class IniciarOportunidade {

        @Test
        void devePermitirInicioQuandoDocenteEstaLogado() {
            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            Docente solicitante = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, solicitante);
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.iniciarOportunidade(oportunidade.getId(), solicitante);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.EM_EXECUCAO);
            assertThat(resultado.getDataInicio()).isNotNull();
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissaoNemDocente() {
            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            Discente solicitante = discente();

            assertThatThrownBy(() -> oportunidadeService.iniciarOportunidade(1, solicitante))
                    .isInstanceOf(SecurityException.class);

            verify(oportunidadeRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoEstaAberta() {
            Docente solicitante = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.RASCUNHO, solicitante);
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            assertThatThrownBy(() -> oportunidadeService.iniciarOportunidade(oportunidade.getId(), solicitante))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deve está aberta");
        }
    }

    // encerrarOportunidade ----------------------------------------------------

    @Nested
    class EncerrarOportunidade {

        @Test
        void devePermitirEncerramentoEDistribuirCargaHorariaAosDiscentes() {
            Docente solicitante = docente();
            Discente participante = discente();
            participante.setCargaHoraria(10);
            Oportunidade oportunidade = oportunidade(StatusOp.EM_EXECUCAO, solicitante);
            oportunidade.getDiscentesOp().add(participante);
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.encerrarOportunidade(oportunidade.getId(), solicitante);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.ENCERRADA);
            assertThat(resultado.getDataFim()).isNotNull();
            assertThat(participante.getCargaHoraria()).isEqualTo(70);
            verify(usuarioRepo, times(1)).saveAll(oportunidade.getDiscentesOp());
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoEstaEmExecucao() {
            Docente solicitante = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, solicitante);
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));

            assertThatThrownBy(() -> oportunidadeService.encerrarOportunidade(oportunidade.getId(), solicitante))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("em execução");
        }
    }

    // cancelarOportunidade ----------------------------------------------------

    @Nested
    class CancelarOportunidade {

        @Test
        void devePermitirCancelamentoQuandoDocenteEstaLogado() {
            Docente solicitante = docente();
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, solicitante);
            when(oportunidadeRepo.findById(oportunidade.getId())).thenReturn(Optional.of(oportunidade));
            when(oportunidadeRepo.save(any(Oportunidade.class))).thenAnswer(inv -> inv.getArgument(0));

            Oportunidade resultado = oportunidadeService.cancelarOportunidade(oportunidade.getId(), solicitante);

            assertThat(resultado.getStatus()).isEqualTo(StatusOp.CANCELADA);
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissaoNemDocente() {
            when(papelRepo.findByNome("ADMIN")).thenReturn(papel("ADMIN"));
            Discente solicitante = discente();

            assertThatThrownBy(() -> oportunidadeService.cancelarOportunidade(1, solicitante))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoOportunidadeNaoExiste() {
            when(oportunidadeRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oportunidadeService.cancelarOportunidade(999, docente()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Oportunidade não encontrada");
        }
    }

    // vagas livres ------------------------------------------------------------

    @Nested
    class VagasLivres {

        @Test
        void deveDiminuirVagasLivres() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente());
            oportunidade.setVagasLivres(2);

            oportunidadeService.diminuiVagasLivres(oportunidade);

            assertThat(oportunidade.getVagasLivres()).isEqualTo(1);
        }

        @Test
        void deveAumentarVagasLivres() {
            Oportunidade oportunidade = oportunidade(StatusOp.ABERTA, docente());
            oportunidade.setVagasLivres(1);

            oportunidadeService.aumentarVagasLivres(oportunidade);

            assertThat(oportunidade.getVagasLivres()).isEqualTo(2);
        }
    }

    // listarOportunidades -------------------------------------------------------

    @Nested
    class ListarOportunidades {

        @Test
        void deveListarTodasAsOportunidades() {
            Oportunidade o = oportunidade(StatusOp.ABERTA, docente());
            when(oportunidadeRepo.findAll()).thenReturn(List.of(o));

            assertThat(oportunidadeService.listarOportunidades()).containsExactly(o);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaOportunidades() {
            when(oportunidadeRepo.findAll()).thenReturn(List.of());

            assertThat(oportunidadeService.listarOportunidades()).isEmpty();
        }
    }
}
