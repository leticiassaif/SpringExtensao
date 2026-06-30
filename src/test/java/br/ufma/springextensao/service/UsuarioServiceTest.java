package br.ufma.springextensao.service;

import br.ufma.springextensao.controller.dtos.DiscenteDTO;
import br.ufma.springextensao.controller.dtos.DocenteDTO;
import br.ufma.springextensao.controller.dtos.PainelHorasDTO;
import br.ufma.springextensao.model.*;
import br.ufma.springextensao.repository.CursoRepo;
import br.ufma.springextensao.repository.PapelRepo;
import br.ufma.springextensao.repository.UsuarioRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepo usuarioRepo;

    @Mock
    CursoRepo cursoRepo;

    @Mock
    PapelRepo papelRepo;

    @Mock
    GrupoService grupoService;

    @InjectMocks
    UsuarioService usuarioService;

    // Helpers ----------------------------------------------------------

    private Papel papel(String nome) {
        Papel p = new Papel();
        p.setId(1);
        p.setNome(nome);
        return p;
    }

    private Discente discenteComCargo(Papel... cargos) {
        Discente d = Discente.builder()
                .nome("Joana")
                .email("joana@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>(List.of(cargos)))
                .matricula("2023001")
                .cargaHoraria(0)
                .grupos(new ArrayList<>())
                .build();
        d.setId(1);
        return d;
    }

    private Docente docenteComCargo(Papel... cargos) {
        Docente doc = Docente.builder()
                .nome("Carlos")
                .email("carlos@ufma.br")
                .senha("hash")
                .ativo(true)
                .cargos(new ArrayList<>(List.of(cargos)))
                .build();
        doc.setId(2);
        return doc;
    }

    // cadastrarDiscente --------------------------------------------------

    @Nested
    class CadastrarDiscente {

        @Test
        void deveCadastrarDiscenteComCursoValido() {
            DiscenteDTO dto = new DiscenteDTO();
            dto.setNome("Maria");
            dto.setEmail("maria@ufma.br");
            dto.setSenha("senha123");
            dto.setMatricula("2024001");
            dto.setCargaHoraria(10);
            dto.setIdCurso(5);

            Curso curso = Curso.builder().id(5).nome("Computação").build();
            when(cursoRepo.findById(5)).thenReturn(Optional.of(curso));
            when(usuarioRepo.save(any(Discente.class))).thenAnswer(inv -> inv.getArgument(0));

            Discente resultado = usuarioService.cadastrarDiscente(dto);

            assertThat(resultado.getNome()).isEqualTo("Maria");
            assertThat(resultado.getEmail()).isEqualTo("maria@ufma.br");
            assertThat(resultado.isAtivo()).isTrue();
            assertThat(resultado.getCurso()).isEqualTo(curso);
            assertThat(resultado.getSenha()).isNotEqualTo("senha123");
            assertThat(new BCryptPasswordEncoder().matches("senha123", resultado.getSenha())).isTrue();

            verify(usuarioRepo).save(any(Discente.class));
        }

        @Test
        void deveLancarExcecaoQuandoCursoNaoExiste() {
            DiscenteDTO dto = new DiscenteDTO();
            dto.setIdCurso(999);
            dto.setSenha("senha123");

            when(cursoRepo.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.cadastrarDiscente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Curso");

            verify(usuarioRepo, never()).save(any());
        }
    }

    // cadastrarDocente ----------------------------------------------------

    @Nested
    class CadastrarDocente {

        @Test
        void deveCadastrarDocenteQuandoSolicitanteEhAdmin() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            DocenteDTO dto = new DocenteDTO();
            dto.setNome("Pedro");
            dto.setEmail("pedro@ufma.br");
            dto.setSenha("minhasenha");
            dto.setSiape("12345");
            dto.setDepartamento("DEINF");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(usuarioRepo.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

            Docente resultado = usuarioService.cadastrarDocente(solicitante, dto);

            assertThat(resultado.getNome()).isEqualTo("Pedro");
            assertThat(resultado.getSiape()).isEqualTo("12345");
            assertThat(resultado.getSenha()).isNotEqualTo("minhasenha");
            assertThat(new BCryptPasswordEncoder().matches("minhasenha", resultado.getSenha())).isTrue();
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhAdmin() {
            Papel docentePapel = papel("DOCENTE");
            Docente solicitante = docenteComCargo(docentePapel);
            Papel admin = papel("ADMIN");

            DocenteDTO dto = new DocenteDTO();
            dto.setSenha("senha");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.cadastrarDocente(solicitante, dto))
                    .isInstanceOf(SecurityException.class);

            verify(usuarioRepo, never()).save(any());
        }
    }

    // promoverDocente -------------------------------------------------------

    @Nested
    class PromoverDocente {

        @Test
        void devePromoverDocenteQuandoSolicitanteEhAdminECargoExiste() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Papel coordenador = papel("COORDENADOR");
            Docente alvo = docenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(2)).thenReturn(Optional.of(alvo));
            when(usuarioRepo.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));

            Docente resultado = usuarioService.promoverDocente(solicitante, "coordenador", 2);

            assertThat(resultado.getCargos()).contains(coordenador);
        }

        @Test
        void deveLancarExcecaoQuandoSolicitanteNaoEhAdmin() {
            Papel docentePapel = papel("DOCENTE");
            Docente solicitante = docenteComCargo(docentePapel);
            Papel admin = papel("ADMIN");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", 2))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        void deveLancarExcecaoQuandoIdEhNulo() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ID");
        }

        @Test
        void deveLancarExcecaoQuandoCargoEhNulo() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, null, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo");
        }

        @Test
        void deveLancarExcecaoQuandoCargoNaoExiste() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("INEXISTENTE")).thenReturn(null);

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "inexistente", 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cargo não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioNaoExiste() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Papel coordenador = papel("COORDENADOR");

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não existe");
        }

        @Test
        void deveLancarExcecaoQuandoUsuarioAlvoNaoEhDocente() {
            Papel admin = papel("ADMIN");
            Docente solicitante = docenteComCargo(admin);
            Papel coordenador = papel("COORDENADOR");
            Discente alvo = discenteComCargo();

            when(papelRepo.findByNome("ADMIN")).thenReturn(admin);
            when(papelRepo.findByNome("COORDENADOR")).thenReturn(coordenador);
            when(usuarioRepo.findById(1)).thenReturn(Optional.of(alvo));

            assertThatThrownBy(() -> usuarioService.promoverDocente(solicitante, "COORDENADOR", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não é docente");
        }
    }
}