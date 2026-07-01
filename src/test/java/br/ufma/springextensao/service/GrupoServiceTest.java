package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.GrupoDTO;
import br.ufma.springextensao.enums.Status;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.GrupoMembroRepo;
import br.ufma.springextensao.repository.GrupoRepo;
import br.ufma.springextensao.repository.PapelRepo;
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
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrupoServiceTest {

    @Mock
    GrupoRepo grupoRepo;

    @Mock
    PapelRepo papelRepo;

    @Mock
    UsuarioRepo usuarioRepo;

    @Mock
    GrupoMembroRepo grupoMembroRepo;

    @Mock
    UsuarioService usuarioService;

    @InjectMocks
    GrupoService grupoService;

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
                .grupos(new ArrayList<>())
                .cargoHistorico(new ArrayList<>())
                .build();
        d.setId(nextId++);
        return d;
    }

    private Grupo grupo(Status status, Docente responsavel) {
        Grupo g = Grupo.builder()
                .nome("Grupo de Extensão")
                .descricao("descrição")
                .email("grupo@ufma.br")
                .status(status)
                .responsavel(responsavel)
                .membros(new ArrayList<>())
                .membrosHistorico(new ArrayList<>())
                .build();
        g.setId(nextId++);
        return g;
    }

    // criar --------------------------------------------------------------

    @Nested
    class Criar {

        @Test
        void deveCriarGrupoPendenteQuandoResponsavelEhDocente() {
            Docente responsavel = docente();
            GrupoDTO dto = new GrupoDTO();
            dto.setNome("Wanda");
            dto.setDescricao("Projeto educacional");
            dto.setEmail("wanda@ufma.br");
            dto.setIdResponsavel(responsavel.getId());

            when(usuarioService.buscarPorId(responsavel.getId())).thenReturn(responsavel);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.criar(dto);

            assertThat(resultado.getNome()).isEqualTo("Wanda");
            assertThat(resultado.getResponsavel()).isEqualTo(responsavel);
            assertThat(resultado.getStatus()).isEqualTo(Status.PENDENTE);
            assertThat(resultado.getMembros()).isEmpty();
        }

        @Test
        void deveLancarExcecaoQuandoResponsavelNaoExiste() {
            GrupoDTO dto = new GrupoDTO();
            dto.setIdResponsavel(999);

            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> grupoService.criar(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não existe");

            verify(grupoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoResponsavelNaoEhDocente() {
            Discente naoDocente = discente();
            GrupoDTO dto = new GrupoDTO();
            dto.setIdResponsavel(naoDocente.getId());

            when(usuarioService.buscarPorId(naoDocente.getId())).thenReturn(naoDocente);

            assertThatThrownBy(() -> grupoService.criar(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é docente");

            verify(grupoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoIdResponsavelEhNulo() {
            GrupoDTO dto = new GrupoDTO();
            // idResponsavel fica null por padrão — usuarioService.buscarPorId(null) no mock retorna null

            assertThatThrownBy(() -> grupoService.criar(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não existe");

            verify(grupoRepo, never()).save(any());
        }

        // BUG CONHECIDO: criar() com DTO null lança NullPointerException ao acessar
        // grupo.getIdResponsavel() sem validação prévia. O esperado é IllegalArgumentException.
        // Fica VERMELHO até a correção.
        @Test
        void deveLancarExcecaoControladaQuandoDtoEhNulo() {
            assertThatThrownBy(() -> grupoService.criar(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NullPointerException.class);
        }
    }

    // aprovar --------------------------------------------------------------

    @Nested
    class Aprovar {

        private void stubPapeisComuns(Papel admin, Papel coordenador, Papel diretor, Papel membro) {
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
            when(papelRepo.findByNome("MEMBRO")).thenReturn(membro);
        }

        private void stubChainPadrao(Grupo grupo, Discente futuroDiretor) {
            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(futuroDiretor.getId())).thenReturn(futuroDiretor);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoMembroRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void deveAprovarQuandoSolicitanteEhResponsavel() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel membro = papel("MEMBRO");
            stubPapeisComuns(admin, coordenador, diretor, membro);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);
            Discente futuroDiretor = discente();
            futuroDiretor.setAtivo(true);

            stubChainPadrao(grupo, futuroDiretor);

            Grupo resultado = grupoService.aprovar(responsavel, grupo.getId(), futuroDiretor.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(resultado.getMembros()).contains(futuroDiretor);
            assertThat(futuroDiretor.getCargos()).contains(diretor);
        }

        @Test
        void deveAprovarQuandoSolicitanteEhAdmin() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel membro = papel("MEMBRO");
            stubPapeisComuns(admin, coordenador, diretor, membro);

            Docente responsavel = docente();
            Docente solicitanteAdmin = docente(admin);
            Grupo grupo = grupo(Status.PENDENTE, responsavel);
            Discente futuroDiretor = discente();

            stubChainPadrao(grupo, futuroDiretor);

            Grupo resultado = grupoService.aprovar(solicitanteAdmin, grupo.getId(), futuroDiretor.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(futuroDiretor.getCargos()).contains(diretor);
        }

        @Test
        void deveAprovarQuandoSolicitanteEhCoordenador() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel membro = papel("MEMBRO");
            stubPapeisComuns(admin, coordenador, diretor, membro);

            Docente responsavel = docente();
            Docente solicitanteCoordenador = docente(coordenador);
            Grupo grupo = grupo(Status.PENDENTE, responsavel);
            Discente futuroDiretor = discente();

            stubChainPadrao(grupo, futuroDiretor);

            Grupo resultado = grupoService.aprovar(solicitanteCoordenador, grupo.getId(), futuroDiretor.getId());

            assertThat(resultado.getStatus()).isEqualTo(Status.APROVADO);
            assertThat(futuroDiretor.getCargos()).contains(diretor);
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoExiste() {
            when(grupoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.aprovar(docente(), 999, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoDiretorNaoExiste() {
            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> grupoService.aprovar(responsavel, grupo.getId(), 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioIndicadoNaoEhDiscente() {
            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);
            Docente naoDiscente = docente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(naoDiscente.getId())).thenReturn(naoDiscente);

            assertThatThrownBy(() -> grupoService.aprovar(responsavel, grupo.getId(), naoDiscente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoEstaPendente() {
            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente futuroDiretor = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(futuroDiretor.getId())).thenReturn(futuroDiretor);

            assertThatThrownBy(() -> grupoService.aprovar(responsavel, grupo.getId(), futuroDiretor.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);

            Docente responsavel = docente();
            Docente outroDocente = docente(); // sem cargos, não é o responsável
            Grupo grupo = grupo(Status.PENDENTE, responsavel);
            Discente futuroDiretor = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(futuroDiretor.getId())).thenReturn(futuroDiretor);

            assertThatThrownBy(() -> grupoService.aprovar(outroDocente, grupo.getId(), futuroDiretor.getId()))
                    .isInstanceOf(SecurityException.class);

            verify(grupoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoDiretorIndicadoEstaInativo() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel membro = papel("MEMBRO");
            stubPapeisComuns(admin, coordenador, diretor, membro);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);
            Discente futuroDiretor = discente();
            futuroDiretor.setAtivo(false);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(futuroDiretor.getId())).thenReturn(futuroDiretor);

            assertThatThrownBy(() -> grupoService.aprovar(responsavel, grupo.getId(), futuroDiretor.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ativo");
        }
    }

    // rejeitar --------------------------------------------------------------

    @Nested
    class Rejeitar {

        @Test
        void adminDeveRejeitarGrupoPendente() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);

            Docente solicitante = docente(admin);
            Grupo grupo = grupo(Status.PENDENTE, docente());

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.rejeitar(solicitante, grupo.getId(), "Documentação incompleta");

            assertThat(resultado.getStatus()).isEqualTo(Status.REJEITADO);
            assertThat(resultado.getJustificativaNegacao()).isEqualTo("Documentação incompleta");
        }

        @Test
        void coordenadorDeveRejeitarGrupoPendente() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);

            Docente solicitante = docente(coordenador);
            Grupo grupo = grupo(Status.PENDENTE, docente());

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.rejeitar(solicitante, grupo.getId(), "Fora do escopo");

            assertThat(resultado.getStatus()).isEqualTo(Status.REJEITADO);
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);

            Docente solicitante = docente(); // sem cargos

            assertThatThrownBy(() -> grupoService.rejeitar(solicitante, 1, "motivo"))
                    .isInstanceOf(SecurityException.class);

            verify(grupoRepo, never()).save(any());
        }

        @Test
        void deveLancarExcecaoQuandoJustificativaNula() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente solicitante = docente(admin);

            assertThatThrownBy(() -> grupoService.rejeitar(solicitante, 1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Justificativa");
        }

        @Test
        void deveLancarExcecaoQuandoJustificativaEmBranco() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente solicitante = docente(admin);

            assertThatThrownBy(() -> grupoService.rejeitar(solicitante, 1, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Justificativa");
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoExiste() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente solicitante = docente(admin);

            when(grupoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.rejeitar(solicitante, 999, "motivo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoEstaPendente() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente solicitante = docente(admin);
            Grupo grupo = grupo(Status.APROVADO, docente());

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            assertThatThrownBy(() -> grupoService.rejeitar(solicitante, grupo.getId(), "motivo"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("não está pendente");
        }
    }

    // adicionarMembro --------------------------------------------------------------

    @Nested
    class AdicionarMembro {

        private void stubPapeis(Papel admin, Papel coordenador, Papel diretor, Papel membro) {
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
            when(papelRepo.findByNome("MEMBRO")).thenReturn(membro);
        }

        @Test
        void deveAdicionarMembroQuandoResponsavelChama() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel membro = papel("MEMBRO");
            stubPapeis(admin, coordenador, diretor, membro);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente novoMembro = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(novoMembro.getId())).thenReturn(novoMembro);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoMembroRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.adicionarMembro(responsavel, grupo.getId(), novoMembro.getId());

            assertThat(resultado.getMembros()).contains(novoMembro);
            assertThat(novoMembro.getGrupos()).contains(grupo);
        }

        @Test
        void deveAdicionarMembroQuandoAdminChama() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel membro = papel("MEMBRO");
            stubPapeis(admin, coordenador, diretor, membro);

            Docente solicitanteAdmin = docente(admin);
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente novoMembro = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(novoMembro.getId())).thenReturn(novoMembro);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.adicionarMembro(solicitanteAdmin, grupo.getId(), novoMembro.getId());

            assertThat(resultado.getMembros()).contains(novoMembro);
        }

        @Test
        void deveAdicionarMembroQuandoCoordenadorChama() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel membro = papel("MEMBRO");
            stubPapeis(admin, coordenador, diretor, membro);

            Docente solicitanteCoordenador = docente(coordenador);
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente novoMembro = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(novoMembro.getId())).thenReturn(novoMembro);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.adicionarMembro(solicitanteCoordenador, grupo.getId(), novoMembro.getId());

            assertThat(resultado.getMembros()).contains(novoMembro);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> grupoService.adicionarMembro(docente(), 1, 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente naoDiscente = docente();
            when(usuarioService.buscarPorId(naoDiscente.getId())).thenReturn(naoDiscente);

            assertThatThrownBy(() -> grupoService.adicionarMembro(docente(), 1, naoDiscente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoExiste() {
            Discente novoMembro = discente();
            when(usuarioService.buscarPorId(novoMembro.getId())).thenReturn(novoMembro);
            when(grupoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.adicionarMembro(docente(), 999, novoMembro.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente outroDocente = docente(); // sem cargos, não é responsável
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente novoMembro = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(novoMembro.getId())).thenReturn(novoMembro);

            assertThatThrownBy(() -> grupoService.adicionarMembro(outroDocente, grupo.getId(), novoMembro.getId()))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoAprovado() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);
            Discente novoMembro = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(novoMembro.getId())).thenReturn(novoMembro);

            assertThatThrownBy(() -> grupoService.adicionarMembro(responsavel, grupo.getId(), novoMembro.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ativo/aprovado");
        }

        @Test
        void deveLancarExcecaoQuandoDiscenteJaEhMembro() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente jaMembro = discente();
            jaMembro.getGrupos().add(grupo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(jaMembro.getId())).thenReturn(jaMembro);

            assertThatThrownBy(() -> grupoService.adicionarMembro(responsavel, grupo.getId(), jaMembro.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("já faz parte");
        }

        @Test
        void deveLancarExcecaoQuandoDiscenteInativo() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente inativo = discente();
            inativo.setAtivo(false);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(inativo.getId())).thenReturn(inativo);

            assertThatThrownBy(() -> grupoService.adicionarMembro(responsavel, grupo.getId(), inativo.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ativo");
        }

        @Test
        void deveLancarExcecaoQuandoIdGrupoEhNulo() {
            Discente novoMembro = discente();
            when(usuarioService.buscarPorId(novoMembro.getId())).thenReturn(novoMembro);

            // buscaPorId(null) no service real lança IAE antes de chegar no repo
            assertThatThrownBy(() -> grupoService.adicionarMembro(docente(), null, novoMembro.getId()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoIdDiscenteEhNulo() {
            // usuarioService.buscarPorId(null) no mock retorna null → "Usuário não existe."
            assertThatThrownBy(() -> grupoService.adicionarMembro(docente(), 1, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // removerMembro --------------------------------------------------------------

    @Nested
    class RemoverMembro {

        private void stubPapeis(Papel admin, Papel coordenador, Papel diretor, Papel vice, Papel tesoureiro) {
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
            when(papelRepo.findByNome("VICE-DIRETOR")).thenReturn(vice);
            when(papelRepo.findByNome("TESOUREIRO")).thenReturn(tesoureiro);
        }

        @Test
        void deveRemoverMembroSemCargosExtras() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel vice = papel("VICE-DIRETOR");
            Papel tesoureiro = papel("TESOUREIRO");
            stubPapeis(admin, coordenador, diretor, vice, tesoureiro);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente membroComum = discente();
            membroComum.getGrupos().add(grupo);
            grupo.getMembros().add(membroComum);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(membroComum.getId())).thenReturn(membroComum);
            when(grupoMembroRepo.findByGrupoAndDiscente(grupo, membroComum)).thenReturn(List.of());
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.removerMembro(responsavel, grupo.getId(), membroComum.getId());

            assertThat(resultado.getMembros()).doesNotContain(membroComum);
            assertThat(membroComum.getGrupos()).doesNotContain(grupo);
        }

        @Test
        void deveRemoverMembroComCargoDiretorRemovendoOCargoTambem() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            Papel vice = papel("VICE-DIRETOR");
            Papel tesoureiro = papel("TESOUREIRO");
            stubPapeis(admin, coordenador, diretor, vice, tesoureiro);

            Docente solicitanteAdmin = docente(admin);
            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente diretorAtual = discente(diretor);
            diretorAtual.getGrupos().add(grupo);
            grupo.getMembros().add(diretorAtual);

            GrupoMembro vinculoDiretor = GrupoMembro.builder()
                    .grupo(grupo)
                    .discente(diretorAtual)
                    .papelExercido(diretor)
                    .dataInicio(LocalDate.now())
                    .build();
            diretorAtual.getCargoHistorico().add(vinculoDiretor);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(diretorAtual.getId())).thenReturn(diretorAtual);
            when(grupoMembroRepo.findByGrupoAndDiscente(grupo, diretorAtual)).thenReturn(List.of(vinculoDiretor));
            when(grupoMembroRepo.findByGrupoAndDiscenteAndPapel(grupo, diretorAtual, diretor))
                    .thenReturn(Optional.of(vinculoDiretor));
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoMembroRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.removerMembro(solicitanteAdmin, grupo.getId(), diretorAtual.getId());

            assertThat(resultado.getMembros()).doesNotContain(diretorAtual);
            assertThat(diretorAtual.getCargos()).doesNotContain(diretor);
            assertThat(vinculoDiretor.getDataFim()).isNotNull();
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoExiste() {
            when(grupoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.removerMembro(docente(), 999, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente outroDocente = docente();
            Grupo grupo = grupo(Status.APROVADO, docente());

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            assertThatThrownBy(() -> grupoService.removerMembro(outroDocente, grupo.getId(), 1))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoAprovado() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            assertThatThrownBy(() -> grupoService.removerMembro(responsavel, grupo.getId(), 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ativo/aprovado");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> grupoService.removerMembro(responsavel, grupo.getId(), 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Docente naoDiscente = docente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(naoDiscente.getId())).thenReturn(naoDiscente);

            assertThatThrownBy(() -> grupoService.removerMembro(responsavel, grupo.getId(), naoDiscente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void deveLancarExcecaoQuandoDiscenteNaoPertenceAoGrupo() {
            Papel admin = papel("ADMIN");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente foraDoGrupo = discente(); // grupos vazio

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(foraDoGrupo.getId())).thenReturn(foraDoGrupo);

            assertThatThrownBy(() -> grupoService.removerMembro(responsavel, grupo.getId(), foraDoGrupo.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não faz parte");
        }
    }

    // atribuirCargo --------------------------------------------------------------

    @Nested
    class AtribuirCargo {

        private void stubPapeis(Papel admin, Papel coordenador, Papel diretor) {
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
        }

        @Test
        void deveAtribuirCargoQuandoResponsavelChama() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel viceDiretor = papel("VICE-DIRETOR");
            stubPapeis(admin, coordenador, papel("DIRETOR"));
            when(papelRepo.findByNome("VICE-DIRETOR")).thenReturn(viceDiretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente membro = discente();
            membro.getGrupos().add(grupo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(membro.getId())).thenReturn(membro);
            when(usuarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoMembroRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            Grupo resultado = grupoService.atribuirCargo(responsavel, membro.getId(), grupo.getId(), "vice-diretor");

            assertThat(membro.getCargos()).contains(viceDiretor);
            assertThat(resultado.getMembrosHistorico()).isNotEmpty();
        }

        @Test
        void deveAtribuirCargoQuandoAdminChama() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel tesoureiro = papel("TESOUREIRO");
            stubPapeis(admin, coordenador, papel("DIRETOR"));
            when(papelRepo.findByNome("TESOUREIRO")).thenReturn(tesoureiro);

            Docente solicitanteAdmin = docente(admin);
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente membro = discente();
            membro.getGrupos().add(grupo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(membro.getId())).thenReturn(membro);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            grupoService.atribuirCargo(solicitanteAdmin, membro.getId(), grupo.getId(), "TESOUREIRO");

            assertThat(membro.getCargos()).contains(tesoureiro);
        }

        @Test
        void deveAtribuirCargoQuandoCoordenadorChama() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel tesoureiro = papel("TESOUREIRO");
            stubPapeis(admin, coordenador, papel("DIRETOR"));
            when(papelRepo.findByNome("TESOUREIRO")).thenReturn(tesoureiro);

            Docente solicitanteCoordenador = docente(coordenador);
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente membro = discente();
            membro.getGrupos().add(grupo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(membro.getId())).thenReturn(membro);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            grupoService.atribuirCargo(solicitanteCoordenador, membro.getId(), grupo.getId(), "TESOUREIRO");

            assertThat(membro.getCargos()).contains(tesoureiro);
        }

        @Test
        void naoDeveDuplicarCargoSeDiscenteJaPossui() {
            Papel admin = papel("ADMIN");
            Papel tesoureiro = papel("TESOUREIRO");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("TESOUREIRO")).thenReturn(tesoureiro);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente membro = discente(tesoureiro); // já possui o cargo
            membro.getGrupos().add(grupo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(membro.getId())).thenReturn(membro);
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            grupoService.atribuirCargo(responsavel, membro.getId(), grupo.getId(), "TESOUREIRO");

            assertThat(membro.getCargos()).containsOnlyOnce(tesoureiro);
        }

        @Test
        void deveLancarExcecaoQuandoCargoNulo() {
            assertThatThrownBy(() -> grupoService.atribuirCargo(docente(), 1, 1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido");
        }

        @Test
        void deveLancarExcecaoQuandoCargoEmBranco() {
            assertThatThrownBy(() -> grupoService.atribuirCargo(docente(), 1, 1, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido");
        }

        @Test
        void deveLancarExcecaoQuandoCargoEhReservado() {
            assertThatThrownBy(() -> grupoService.atribuirCargo(docente(), 1, 1, "ADMIN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não pode ser atribuido");

            assertThatThrownBy(() -> grupoService.atribuirCargo(docente(), 1, 1, "COORDENADOR"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> grupoService.atribuirCargo(docente(), 1, 1, "MEMBRO"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deveLancarExcecaoQuandoPapelNaoExiste() {
            when(papelRepo.findByNome("INEXISTENTE")).thenReturn(null);

            assertThatThrownBy(() -> grupoService.atribuirCargo(docente(), 1, 1, "inexistente"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Papel não existe");
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoExiste() {
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
            when(grupoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.atribuirCargo(docente(), 1, 999, "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            stubPapeis(admin, coordenador, diretor);

            Docente outroDocente = docente();
            Grupo grupo = grupo(Status.APROVADO, docente());

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            assertThatThrownBy(() -> grupoService.atribuirCargo(outroDocente, 1, grupo.getId(), "DIRETOR"))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoAprovado() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            assertThatThrownBy(() -> grupoService.atribuirCargo(responsavel, 1, grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ativo/aprovado");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> grupoService.atribuirCargo(responsavel, 999, grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Docente naoDiscente = docente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(naoDiscente.getId())).thenReturn(naoDiscente);

            assertThatThrownBy(() -> grupoService.atribuirCargo(responsavel, naoDiscente.getId(), grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void deveLancarExcecaoQuandoDiscenteNaoPertenceAoGrupo() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente foraDoGrupo = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(foraDoGrupo.getId())).thenReturn(foraDoGrupo);

            assertThatThrownBy(() -> grupoService.atribuirCargo(responsavel, foraDoGrupo.getId(), grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não faz parte");
        }
    }

    // removerCargo --------------------------------------------------------------

    @Nested
    class RemoverCargo {

        @Test
        void deveRemoverCargoDoDiscenteQuandoUnico() {
            Papel admin = papel("ADMIN");
            Papel tesoureiro = papel("TESOUREIRO");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("TESOUREIRO")).thenReturn(tesoureiro);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente discente = discente(tesoureiro);

            GrupoMembro vinculo = GrupoMembro.builder()
                    .grupo(grupo).discente(discente).papelExercido(tesoureiro)
                    .dataInicio(LocalDate.now()).build();
            discente.getCargoHistorico().add(vinculo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(grupoMembroRepo.findByGrupoAndDiscenteAndPapel(grupo, discente, tesoureiro))
                    .thenReturn(Optional.of(vinculo));
            when(usuarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoMembroRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            grupoService.removerCargo(responsavel, discente.getId(), grupo.getId(), "TESOUREIRO");

            assertThat(discente.getCargos()).doesNotContain(tesoureiro);
            assertThat(vinculo.getDataFim()).isNotNull();
        }

        @Test
        void naoDeveRemoverCargoGeralQuandoDiscentePossuiEmOutroGrupoAtivo() {
            Papel admin = papel("ADMIN");
            Papel tesoureiro = papel("TESOUREIRO");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("TESOUREIRO")).thenReturn(tesoureiro);

            Docente responsavel = docente();
            Grupo grupoA = grupo(Status.APROVADO, responsavel);
            Grupo grupoB = grupo(Status.APROVADO, docente());
            Discente discente = discente(tesoureiro);

            GrupoMembro vinculoGrupoA = GrupoMembro.builder()
                    .grupo(grupoA).discente(discente).papelExercido(tesoureiro)
                    .dataInicio(LocalDate.now()).build();
            GrupoMembro vinculoGrupoBAtivo = GrupoMembro.builder()
                    .grupo(grupoB).discente(discente).papelExercido(tesoureiro)
                    .dataInicio(LocalDate.now()).dataFim(null).build();
            discente.getCargoHistorico().add(vinculoGrupoA);
            discente.getCargoHistorico().add(vinculoGrupoBAtivo);

            when(grupoRepo.findById(grupoA.getId())).thenReturn(Optional.of(grupoA));
            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(grupoMembroRepo.findByGrupoAndDiscenteAndPapel(grupoA, discente, tesoureiro))
                    .thenReturn(Optional.of(vinculoGrupoA));
            when(grupoMembroRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(grupoRepo.save(any(Grupo.class))).thenAnswer(inv -> inv.getArgument(0));

            grupoService.removerCargo(responsavel, discente.getId(), grupoA.getId(), "TESOUREIRO");

            // ainda possui o cargo geral, pois está ativo em outro grupo
            assertThat(discente.getCargos()).contains(tesoureiro);
            assertThat(vinculoGrupoA.getDataFim()).isNotNull();
            verify(usuarioRepo, never()).save(discente);
        }

        @Test
        void deveLancarExcecaoQuandoCargoNulo() {
            assertThatThrownBy(() -> grupoService.removerCargo(docente(), 1, 1, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido");
        }

        @Test
        void deveLancarExcecaoQuandoCargoEmBranco() {
            assertThatThrownBy(() -> grupoService.removerCargo(docente(), 1, 1, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo inválido");
        }

        @Test
        void deveLancarExcecaoQuandoPapelNaoExiste() {
            when(papelRepo.findByNome("INEXISTENTE")).thenReturn(null);

            assertThatThrownBy(() -> grupoService.removerCargo(docente(), 1, 1, "inexistente"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Papel não existe");
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoExiste() {
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);
            when(grupoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.removerCargo(docente(), 1, 999, "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoSemPermissao() {
            Papel admin = papel("ADMIN");
            Papel coordenador = papel("COORDENADOR");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente outroDocente = docente();
            Grupo grupo = grupo(Status.APROVADO, docente());

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            assertThatThrownBy(() -> grupoService.removerCargo(outroDocente, 1, grupo.getId(), "DIRETOR"))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoGrupoNaoAprovado() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.PENDENTE, responsavel);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            assertThatThrownBy(() -> grupoService.removerCargo(responsavel, 1, grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ativo/aprovado");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> grupoService.removerCargo(responsavel, 999, grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Docente naoDiscente = docente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(naoDiscente.getId())).thenReturn(naoDiscente);

            assertThatThrownBy(() -> grupoService.removerCargo(responsavel, naoDiscente.getId(), grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void deveLancarExcecaoQuandoDiscenteNaoPossuiCargo() {
            Papel admin = papel("ADMIN");
            Papel diretor = papel("DIRETOR");
            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("DIRETOR")).thenReturn(diretor);

            Docente responsavel = docente();
            Grupo grupo = grupo(Status.APROVADO, responsavel);
            Discente semCargo = discente();

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));
            when(usuarioService.buscarPorId(semCargo.getId())).thenReturn(semCargo);
            when(grupoMembroRepo.findByGrupoAndDiscenteAndPapel(grupo, semCargo, diretor))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.removerCargo(responsavel, semCargo.getId(), grupo.getId(), "DIRETOR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não possui esse cargo");
        }
    }

    // removerDiscenteTodosGrupos --------------------------------------------------------------

    @Nested
    class RemoverDiscenteTodosGrupos {

        @Test
        void deveRemoverDiscenteDeTodosOsGrupos() {
            Discente discente = discente();
            Grupo grupoA = grupo(Status.APROVADO, docente());
            Grupo grupoB = grupo(Status.APROVADO, docente());
            discente.getGrupos().add(grupoA);
            discente.getGrupos().add(grupoB);
            grupoA.getMembros().add(discente);
            grupoB.getMembros().add(discente);

            GrupoMembro vinculoA = GrupoMembro.builder().grupo(grupoA).discente(discente).build();
            GrupoMembro vinculoB = GrupoMembro.builder().grupo(grupoB).discente(discente).build();

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(grupoMembroRepo.findByDiscente(discente)).thenReturn(List.of(vinculoA, vinculoB));

            grupoService.removerDiscenteTodosGrupos(docente(), discente.getId());

            assertThat(discente.getGrupos()).isEmpty();
            assertThat(grupoA.getMembros()).doesNotContain(discente);
            assertThat(grupoB.getMembros()).doesNotContain(discente);
            assertThat(vinculoA.getDataFim()).isNotNull();
            assertThat(vinculoB.getDataFim()).isNotNull();
            verify(usuarioRepo).save(discente);
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            when(usuarioService.buscarPorId(999)).thenReturn(null);

            assertThatThrownBy(() -> grupoService.removerDiscenteTodosGrupos(docente(), 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoEhDiscente() {
            Docente naoDiscente = docente();
            when(usuarioService.buscarPorId(naoDiscente.getId())).thenReturn(naoDiscente);

            assertThatThrownBy(() -> grupoService.removerDiscenteTodosGrupos(docente(), naoDiscente.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é discente");
        }

        @Test
        void deveIgnorarDiscenteSemGrupos() {
            Discente discente = discente();
            // grupos já é ArrayList vazia pelo helper

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(grupoMembroRepo.findByDiscente(discente)).thenReturn(List.of());

            grupoService.removerDiscenteTodosGrupos(docente(), discente.getId());

            verify(usuarioRepo).save(discente);
            verify(grupoRepo).saveAll(any());
        }

        // BUG CONHECIDO: removerDiscenteTodosGrupos() chama grupoRepo.saveAll(discente.getGrupos())
        // APÓS discente.getGrupos().clear(), portanto persiste lista vazia. As remoções nos grupos
        // nunca chegam ao banco. Fica VERMELHO até a correção.
        @Test
        void deveAtualizarGruposNoRepositorioAoRemoverDiscente() {
            Discente discente = discente();
            Grupo grupoA = grupo(Status.APROVADO, docente());
            Grupo grupoB = grupo(Status.APROVADO, docente());
            discente.getGrupos().add(grupoA);
            discente.getGrupos().add(grupoB);
            grupoA.getMembros().add(discente);
            grupoB.getMembros().add(discente);

            GrupoMembro vinculoA = GrupoMembro.builder().grupo(grupoA).discente(discente).build();
            GrupoMembro vinculoB = GrupoMembro.builder().grupo(grupoB).discente(discente).build();

            when(usuarioService.buscarPorId(discente.getId())).thenReturn(discente);
            when(grupoMembroRepo.findByDiscente(discente)).thenReturn(List.of(vinculoA, vinculoB));

            grupoService.removerDiscenteTodosGrupos(docente(), discente.getId());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Grupo>> captor = ArgumentCaptor.forClass((Class) Iterable.class);
            verify(grupoRepo).saveAll(captor.capture());

            List<Grupo> salvos = new ArrayList<>();
            captor.getValue().forEach(salvos::add);
            assertThat(salvos).containsExactlyInAnyOrder(grupoA, grupoB);
        }
    }

    // buscaPorId --------------------------------------------------------------

    @Nested
    class BuscaPorId {

        @Test
        void deveRetornarGrupoQuandoExiste() {
            Grupo grupo = grupo(Status.APROVADO, docente());
            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            Grupo resultado = grupoService.buscaPorId(grupo.getId());

            assertThat(resultado).isEqualTo(grupo);
        }

        @Test
        void deveRetornarNuloQuandoNaoExiste() {
            when(grupoRepo.findById(404)).thenReturn(Optional.empty());

            Grupo resultado = grupoService.buscaPorId(404);

            assertThat(resultado).isNull();
        }

        @Test
        void deveLancarExcecaoQuandoIdNulo() {
            assertThatThrownBy(() -> grupoService.buscaPorId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");

            verifyNoInteractions(grupoRepo);
        }
    }

    // listas --------------------------------------------------------------

    @Nested
    class Listas {

        @Test
        void deveListarTodosOsGrupos() {
            Grupo grupoA = grupo(Status.APROVADO, docente());
            Grupo grupoB = grupo(Status.PENDENTE, docente());
            when(grupoRepo.findAll()).thenReturn(List.of(grupoA, grupoB));

            List<Grupo> resultado = grupoService.listaGrupos();

            assertThat(resultado).containsExactly(grupoA, grupoB);
        }

        @Test
        void deveListarMembrosDoGrupo() {
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente membro = discente();
            grupo.getMembros().add(membro);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            List<Discente> resultado = grupoService.listaGrupoMembros(grupo.getId());

            assertThat(resultado).containsExactly(membro);
        }

        @Test
        void deveLancarExcecaoAoListarMembrosDeGrupoInexistente() {
            when(grupoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> grupoService.listaGrupoMembros(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Grupo não existe");
        }

        @Test
        void deveListarApenasMembrosAtivos() {
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente ativo = discente();
            Discente inativo = discente();
            inativo.setAtivo(false);
            grupo.getMembros().add(ativo);
            grupo.getMembros().add(inativo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            List<Discente> resultado = grupoService.listaGrupoMembrosAtivos(grupo.getId());

            assertThat(resultado).containsExactly(ativo);
        }

        @Test
        void deveListarApenasMembrosNaoAtivos() {
            Grupo grupo = grupo(Status.APROVADO, docente());
            Discente ativo = discente();
            Discente inativo = discente();
            inativo.setAtivo(false);
            grupo.getMembros().add(ativo);
            grupo.getMembros().add(inativo);

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            List<Discente> resultado = grupoService.listaGrupoMembrosNaoAtivos(grupo.getId());

            assertThat(resultado).containsExactly(inativo);
        }

        @Test
        void deveLancarExcecaoAoListarMembrosDeGrupoComIdNulo() {
            assertThatThrownBy(() -> grupoService.listaGrupoMembros(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID inválido");

            verifyNoInteractions(grupoRepo);
        }

        @Test
        void deveRetornarListaVaziaParaMembrosAtivosDeGrupoSemMembros() {
            Grupo grupo = grupo(Status.APROVADO, docente());
            // membros já é ArrayList vazia pelo helper

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            List<Discente> resultado = grupoService.listaGrupoMembrosAtivos(grupo.getId());

            assertThat(resultado).isEmpty();
        }

        @Test
        void deveRetornarListaVaziaParaMembrosNaoAtivosDeGrupoSemMembros() {
            Grupo grupo = grupo(Status.APROVADO, docente());

            when(grupoRepo.findById(grupo.getId())).thenReturn(Optional.of(grupo));

            List<Discente> resultado = grupoService.listaGrupoMembrosNaoAtivos(grupo.getId());

            assertThat(resultado).isEmpty();
        }
    }
}