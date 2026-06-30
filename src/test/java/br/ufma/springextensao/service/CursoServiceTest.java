package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.CursoDTO;
import br.ufma.springextensao.controller.dtos.UCEDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UCERepo;
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
class CursoServiceTest {

    @Mock
    CursoRepo cursoRepo;

    @Mock
    UCERepo uceRepo;

    @Mock
    PapelRepo papelRepo;

    @InjectMocks
    CursoService cursoService;

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

    private void stubPapeisPermissao(Papel admin, Papel coordenador) {
        when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
        when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
    }

    // criarCurso --------------------------------------------------------------

    @Nested
    class CriarCurso {

        @Test
        void devePermitirAdminCriarCurso() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2025", "PPC2025", 3200, null, null);

            when(cursoRepo.findVigente()).thenReturn(null);
            when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

            Curso resultado = cursoService.criarCurso(solicitante, dto);

            assertThat(resultado.getCodigo()).isEqualTo("CC-2025");
            assertThat(resultado.getCurriculo()).isEqualTo("PPC2025");
            assertThat(resultado.getCargaHoraria()).isEqualTo(3200);
            assertThat(resultado.getDataInicio()).isEqualTo(LocalDate.now());
            assertThat(resultado.getDataFim()).isNull();
        }

        @Test
        void devePermitirCoordenadorCriarCurso() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(coordenador);
            CursoDTO dto = cursoDTO("CC-2025", "PPC2025", 3200, null, null);

            when(cursoRepo.findVigente()).thenReturn(null);
            when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

            Curso resultado = cursoService.criarCurso(solicitante, dto);

            assertThat(resultado.getCodigo()).isEqualTo("CC-2025");
            assertThat(resultado.getDataInicio()).isEqualTo(LocalDate.now());
        }

        @Test
        void deveEncerrarCursoAnteriorAoCriarNovoVigente() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            Curso anterior = curso("PPC2020");
            CursoDTO dto = cursoDTO("CC-2025", "PPC2025", 3200, null, null);

            when(cursoRepo.findVigente()).thenReturn(anterior);
            when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

            cursoService.criarCurso(solicitante, dto);

            assertThat(anterior.getDataFim()).isEqualTo(LocalDate.now());
            // save é chamado duas vezes: uma para fechar o anterior, outra para o novo
            verify(cursoRepo, times(2)).save(any(Curso.class));
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente semCargo = docente();

            assertThatThrownBy(() -> cursoService.criarCurso(semCargo, cursoDTO("CC", "PPC", 3200, null, null)))
                    .isInstanceOf(SecurityException.class);

            verify(cursoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoDiscenteEhSolicitante() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Discente discenteSolicitante = discente();

            assertThatThrownBy(() -> cursoService.criarCurso(discenteSolicitante, cursoDTO("CC", "PPC", 3200, null, null)))
                    .isInstanceOf(SecurityException.class);

            verify(cursoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteEhNulo() {
            assertThatThrownBy(() -> cursoService.criarCurso(null, cursoDTO("CC", "PPC", 3200, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário inválido");
        }

        // BUG CONHECIDO: criarCurso() lança NullPointerException quando o DTO é null,
        // pois curso.getCodigo() é chamado sem validação prévia. O esperado é uma
        // exceção controlada. Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoControladaQuandoDtoEhNulo() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);

            assertThatThrownBy(() -> cursoService.criarCurso(solicitante, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }

        // BUG CONHECIDO: criarCurso() aceita cargaHoraria negativa/zero sem validação;
        // um curso com cargaHoraria = -100 é persistido sem erro. O esperado é rejeitar
        // com IllegalArgumentException. Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoQuandoCargaHorariaEhNegativa() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2025", "PPC2025", -100, null, null);

            when(cursoRepo.findVigente()).thenReturn(null);

            assertThatThrownBy(() -> cursoService.criarCurso(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(cursoRepo, never()).save(any());
        }

        // BUG CONHECIDO: criarCurso() aceita cargaHoraria = 0 sem validação.
        // Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoQuandoCargaHorariaEhZero() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2025", "PPC2025", 0, null, null);

            when(cursoRepo.findVigente()).thenReturn(null);

            assertThatThrownBy(() -> cursoService.criarCurso(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(cursoRepo, never()).save(any());
        }
    }

    // cadastrarCurso --------------------------------------------------------------

    @Nested
    class CadastrarCurso {

        @Test
        void devePermitirAdminCadastrarCursoHistorico() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "2020-01-01", "2025-01-01");

            when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

            Curso resultado = cursoService.cadastrarCurso(solicitante, dto);

            assertThat(resultado.getCodigo()).isEqualTo("CC-2020");
            assertThat(resultado.getCurriculo()).isEqualTo("PPC2020");
            assertThat(resultado.getDataInicio()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(resultado.getDataFim()).isEqualTo(LocalDate.of(2025, 1, 1));
        }

        @Test
        void devePermitirCoordenadorCadastrarCursoHistorico() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(coordenador);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "2020-01-01", "2025-01-01");

            when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

            Curso resultado = cursoService.cadastrarCurso(solicitante, dto);

            assertThat(resultado.getDataInicio()).isEqualTo(LocalDate.of(2020, 1, 1));
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente semCargo = docente();

            assertThatThrownBy(() -> cursoService.cadastrarCurso(semCargo, cursoDTO("CC", "PPC", 3200, "2020-01-01", "2025-01-01")))
                    .isInstanceOf(SecurityException.class);

            verify(cursoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteEhNulo() {
            assertThatThrownBy(() -> cursoService.cadastrarCurso(null, cursoDTO("CC", "PPC", 3200, "2020-01-01", "2025-01-01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário inválido");
        }

        @Test
        void deveLancarExcecaoQuandoDataInicioEhTextoInvalido() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "01/01/2020", "2025-01-01");

            assertThatThrownBy(() -> cursoService.cadastrarCurso(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoDataFimEhTextoInvalido() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "2020-01-01", "01/01/2025");

            assertThatThrownBy(() -> cursoService.cadastrarCurso(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        // BUG CONHECIDO: cadastrarCurso() lança NullPointerException quando dataInicio é null,
        // porque LocalDate.parse(null) joga NullPointerException, que não é capturado pelo
        // catch(DateTimeParseException) em Validacao.formataDataIso(). O esperado é
        // IllegalArgumentException controlada. Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoControladaQuandoDataInicioEhNula() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, null, "2025-01-01");

            assertThatThrownBy(() -> cursoService.cadastrarCurso(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }

        // BUG CONHECIDO: cadastrarCurso() lança NullPointerException quando dataFim é null,
        // pelo mesmo motivo acima. Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoControladaQuandoDataFimEhNula() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "2020-01-01", null);

            assertThatThrownBy(() -> cursoService.cadastrarCurso(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }

        // BUG CONHECIDO: cadastrarCurso() lança NullPointerException quando o DTO é null.
        // O esperado é uma exceção controlada. Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoControladaQuandoDtoEhNulo() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);

            assertThatThrownBy(() -> cursoService.cadastrarCurso(solicitante, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }

        // BUG CONHECIDO: cadastrarCurso() não inclui nome nem cargaHoraria no Curso gerado,
        // ao contrário de criarCurso(). O curso histórico nasce com nome == null e
        // cargaHoraria == null. O esperado é que ambos sejam persistidos.
        // Fica VERMELHO até a correção.
        @Test
        void devePersistirNomeECargaHorariaNoCursoHistorico() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            CursoDTO dto = cursoDTO("CC-2020", "PPC2020", 3200, "2020-01-01", "2025-01-01");

            when(cursoRepo.save(any(Curso.class))).thenAnswer(inv -> inv.getArgument(0));

            Curso resultado = cursoService.cadastrarCurso(solicitante, dto);

            assertThat(resultado.getCargaHoraria()).isEqualTo(3200);
            assertThat(resultado.getNome()).isNotNull();
        }
    }

    // buscarPorVersao --------------------------------------------------------------

    @Nested
    class BuscarPorVersao {

        @Test
        void deveRetornarCursoQuandoVersaoExiste() {
            Curso curso = curso("PPC2025");
            when(cursoRepo.findByVersao("PPC2025")).thenReturn(Optional.of(curso));

            Curso resultado = cursoService.buscarPorVersao("PPC2025");

            assertThat(resultado).isEqualTo(curso);
        }

        @Test
        void deveRetornarNuloQuandoVersaoNaoExiste() {
            when(cursoRepo.findByVersao("INEXISTENTE")).thenReturn(Optional.empty());

            Curso resultado = cursoService.buscarPorVersao("INEXISTENTE");

            assertThat(resultado).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoVersaoEhNula() {
            assertThatThrownBy(() -> cursoService.buscarPorVersao(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Versão inválida");

            verifyNoInteractions(cursoRepo);
        }

        // BUG CONHECIDO: buscarPorVersao() não valida string em branco; uma versão como
        // "   " passa direto para o banco sem erro (inconsistente com outros services
        // que usam isBlank()). O esperado é rejeitar com IllegalArgumentException.
        // Fica VERMELHO até a validação ser adicionada.
        @Test
        void deveLancarExcecaoQuandoVersaoEhEmBranco() {
            assertThatThrownBy(() -> cursoService.buscarPorVersao("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // buscarVigente --------------------------------------------------------------

    @Nested
    class BuscarVigente {

        @Test
        void deveRetornarCursoVigente() {
            Curso vigente = curso("PPC2025");
            when(cursoRepo.findVigente()).thenReturn(vigente);

            Curso resultado = cursoService.buscarVigente();

            assertThat(resultado).isEqualTo(vigente);
        }

        @Test
        void deveRetornarNuloQuandoNaoHaVigente() {
            when(cursoRepo.findVigente()).thenReturn(null);

            Curso resultado = cursoService.buscarVigente();

            assertThat(resultado).isNull();
        }
    }

    // buscaPorId --------------------------------------------------------------

    @Nested
    class BuscaPorId {

        @Test
        void deveRetornarCursoQuandoExiste() {
            Curso curso = curso("PPC2025");
            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));

            Curso resultado = cursoService.buscaPorId(curso.getId());

            assertThat(resultado).isEqualTo(curso);
        }

        @Test
        void deveRetornarNuloQuandoNaoExiste() {
            when(cursoRepo.findById(404)).thenReturn(Optional.empty());

            Curso resultado = cursoService.buscaPorId(404);

            assertThat(resultado).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            assertThatThrownBy(() -> cursoService.buscaPorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");

            verifyNoInteractions(cursoRepo);
        }
    }

    // listaHistorico --------------------------------------------------------------

    @Nested
    class ListaHistorico {

        @Test
        void deveRetornarTodosOsCursos() {
            Curso cursoA = curso("PPC2020");
            Curso cursoB = curso("PPC2025");
            when(cursoRepo.findAll()).thenReturn(List.of(cursoA, cursoB));

            List<Curso> resultado = cursoService.listaHistorico();

            assertThat(resultado).containsExactly(cursoA, cursoB);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaCursos() {
            when(cursoRepo.findAll()).thenReturn(List.of());

            List<Curso> resultado = cursoService.listaHistorico();

            assertThat(resultado).isEmpty();
        }
    }

    // cadastrarUCE --------------------------------------------------------------

    @Nested
    class CadastrarUCE {

        @Test
        void devePermitirAdminCadastrarUCE() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            Curso curso = curso("PPC2025");
            UCEDTO dto = uceDTO("Cálculo I", 60, curso.getId());

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));
            when(uceRepo.save(any(UCE.class))).thenAnswer(inv -> inv.getArgument(0));

            UCE resultado = cursoService.cadastrarUCE(solicitante, dto);

            assertThat(resultado.getNome()).isEqualTo("Cálculo I");
            assertThat(resultado.getCargaHoraria()).isEqualTo(60);
            assertThat(resultado.getCurso()).isEqualTo(curso);
        }

        @Test
        void devePermitirCoordenadorCadastrarUCE() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(coordenador);
            Curso curso = curso("PPC2025");
            UCEDTO dto = uceDTO("Álgebra Linear", 60, curso.getId());

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));
            when(uceRepo.save(any(UCE.class))).thenAnswer(inv -> inv.getArgument(0));

            UCE resultado = cursoService.cadastrarUCE(solicitante, dto);

            assertThat(resultado.getNome()).isEqualTo("Álgebra Linear");
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente semCargo = docente();
            UCEDTO dto = uceDTO("Cálculo I", 60, 1);

            assertThatThrownBy(() -> cursoService.cadastrarUCE(semCargo, dto))
                    .isInstanceOf(SecurityException.class);

            verify(uceRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteEhNulo() {
            assertThatThrownBy(() -> cursoService.cadastrarUCE(null, uceDTO("Cálculo I", 60, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário inválido");
        }

        @Test
        void deveLancarExcecaoQuandoCursoNaoExiste() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            UCEDTO dto = uceDTO("Cálculo I", 60, 999);

            when(cursoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.cadastrarUCE(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso não existe");

            verify(uceRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoIdCursoEhNulo() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            UCEDTO dto = uceDTO("Cálculo I", 60, null);

            assertThatThrownBy(() -> cursoService.cadastrarUCE(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");

            verify(uceRepo, never()).save(any());
        }

        // BUG CONHECIDO: cadastrarUCE() não valida se o nome da UCE é null; uma UCE com
        // nome == null é persistida sem erro. O esperado é rejeitar com
        // IllegalArgumentException. Fica VERMELHO até a validação ser adicionada.
        @Test
        void deveLancarExcecaoQuandoNomeDaUCEEhNulo() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            Curso curso = curso("PPC2025");
            UCEDTO dto = uceDTO(null, 60, curso.getId());

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));

            assertThatThrownBy(() -> cursoService.cadastrarUCE(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(uceRepo, never()).save(any());
        }

        // BUG CONHECIDO: cadastrarUCE() não valida se o nome da UCE é em branco.
        // Fica VERMELHO até a validação ser adicionada.
        @Test
        void deveLancarExcecaoQuandoNomeDaUCEEhEmBranco() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            Curso curso = curso("PPC2025");
            UCEDTO dto = uceDTO("   ", 60, curso.getId());

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));

            assertThatThrownBy(() -> cursoService.cadastrarUCE(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(uceRepo, never()).save(any());
        }

        // BUG CONHECIDO: cadastrarUCE() não valida cargaHoraria negativa; uma UCE com
        // cargaHoraria = -30 é persistida sem erro. Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoQuandoCargaHorariaDaUCEEhNegativa() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            Curso curso = curso("PPC2025");
            UCEDTO dto = uceDTO("Cálculo I", -30, curso.getId());

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));

            assertThatThrownBy(() -> cursoService.cadastrarUCE(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(uceRepo, never()).save(any());
        }

        // BUG CONHECIDO: cadastrarUCE() não valida cargaHoraria = 0.
        // Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoQuandoCargaHorariaDaUCEEhZero() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            stubPapeisPermissao(admin, coordenador);

            Docente solicitante = docente(admin);
            Curso curso = curso("PPC2025");
            UCEDTO dto = uceDTO("Cálculo I", 0, curso.getId());

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));

            assertThatThrownBy(() -> cursoService.cadastrarUCE(solicitante, dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(uceRepo, never()).save(any());
        }
    }

    // buscaUCEPorPPC --------------------------------------------------------------

    @Nested
    class BuscaUCEPorPPC {

        @Test
        void deveRetornarUCEsDoPrograma() {
            Curso curso = curso("PPC2025");
            UCE uce1 = UCE.builder().nome("Cálculo I").cargaHoraria(60).curso(curso).build();
            UCE uce2 = UCE.builder().nome("Álgebra Linear").cargaHoraria(60).curso(curso).build();

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));
            when(uceRepo.findByCurso(curso)).thenReturn(List.of(uce1, uce2));

            List<UCE> resultado = cursoService.buscaUCEPorPPC(curso.getId());

            assertThat(resultado).containsExactly(uce1, uce2);
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoHaUCEs() {
            Curso curso = curso("PPC2025");

            when(cursoRepo.findById(curso.getId())).thenReturn(Optional.of(curso));
            when(uceRepo.findByCurso(curso)).thenReturn(List.of());

            List<UCE> resultado = cursoService.buscaUCEPorPPC(curso.getId());

            assertThat(resultado).isEmpty();
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            assertThatThrownBy(() -> cursoService.buscaUCEPorPPC(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");

            verifyNoInteractions(cursoRepo);
        }

        @Test
        void deveLancarExcecaoQuandoCursoNaoExiste() {
            when(cursoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.buscaUCEPorPPC(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso não existe");
        }
    }
}
