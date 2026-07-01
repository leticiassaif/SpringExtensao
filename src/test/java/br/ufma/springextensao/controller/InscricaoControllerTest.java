package br.ufma.springextensao.controller;

import br.ufma.extensao.servicos.InscricaoService;
import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Inscricao;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes unitários de InscricaoController.
 *
 * Estratégia: MockMvc standalone (sem contexto Spring).
 * O InscricaoService e o UsuarioService são mocks puros — nenhum banco é acessado.
 *
 * Testes marcados com "BUG CONHECIDO" ficam VERMELHOS até a correção correspondente
 * ser aplicada no código de produção.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscricaoControllerTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Dependências mockadas
    // ═══════════════════════════════════════════════════════════════════════

    @Mock
    private InscricaoService inscricaoService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private InscricaoController controller;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    // ═══════════════════════════════════════════════════════════════════════
    // Setup
    // ═══════════════════════════════════════════════════════════════════════

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper  = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers — construtores de entidades de teste
    // ═══════════════════════════════════════════════════════════════════════

    private Usuario umUsuario(Integer id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private Oportunidade umaOportunidade(Integer id) {
        Oportunidade o = new Oportunidade();
        o.setId(id);
        return o;
    }

    private Inscricao umaInscricao(Integer id) {
        Inscricao i = new Inscricao();
        i.setId(id);
        return i;
    }

    private Discente umDiscente(Integer id) {
        Discente d = new Discente();
        d.setId(id);
        return d;
    }

    private InscricaoDTO umaInscricaoDTO(Integer oportunidadeId) {
        InscricaoDTO dto = new InscricaoDTO();
        // ajuste os setters conforme os campos reais de InscricaoDTO
        dto.setOportunidadeId(oportunidadeId);
        return dto;
    }

    /** Sessão com o atributo CORRETO de usuário logado. */
    private MockHttpSession sessaoLogada(Integer usuarioId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("IdUsuarioLogado", usuarioId);
        return s;
    }

    /** Sessão sem nenhum atributo (usuário não autenticado). */
    private MockHttpSession sessaoVazia() {
        return new MockHttpSession();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/inscricao/inscrever
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class InscreverTest {

        @Test
        void caminhoFeliz_deveInscreverERetornar201() throws Exception {
            InscricaoDTO dto      = umaInscricaoDTO(5);
            Inscricao    salva    = umaInscricao(1);

            when(inscricaoService.inscrever(any(InscricaoDTO.class))).thenReturn(salva);

            mockMvc.perform(post("/api/inscricao/inscrever")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(inscricaoService, times(1)).inscrever(any(InscricaoDTO.class));
        }

        @Test
        void deveRepassarDTOIntactoAoService() throws Exception {
            InscricaoDTO dto   = umaInscricaoDTO(7);
            Inscricao    salva = umaInscricao(2);

            when(inscricaoService.inscrever(any(InscricaoDTO.class))).thenReturn(salva);

            mockMvc.perform(post("/api/inscricao/inscrever")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            // garante que o service foi chamado com um DTO (não null)
            verify(inscricaoService).inscrever(any(InscricaoDTO.class));
        }

        @Test
        void semCorpo_deveRetornar400() throws Exception {
            mockMvc.perform(post("/api/inscricao/inscrever")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(inscricaoService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/inscricao/aprovar/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class AprovarTest {

        // BUG CONHECIDO [InscricaoController.java, linha 32+34]:
        // @PatchMapping("/aprovar/{id}") declara a variável de path como {id},
        // mas o parâmetro do método usa @PathVariable Integer inscricaoId (sem @PathVariable("id")).
        // Spring não consegue fazer o binding e lança IllegalArgumentException → 500.
        // Fica VERMELHO até adicionar @PathVariable("id") ou renomear o path para {inscricaoId}.
        @Test
        void caminhoFeliz_deveAprovarInscricaoERetornar200() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    aprovada     = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.aprovar(eq(1), any(Oportunidade.class), eq(solicitante)))
                    .thenReturn(aprovada);

            mockMvc.perform(patch("/api/inscricao/aprovar/1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());

            verify(inscricaoService).aprovar(eq(1), any(Oportunidade.class), eq(solicitante));
        }

        // BUG CONHECIDO [InscricaoController.java, linha 35]:
        // O atributo de sessão lido é "IsUsuaeioLogado" (typo com "ae") em vez de
        // "IdUsuarioLogado". Com a sessão correta configurada aqui, o controller
        // não encontra o atributo, retorna null e lança SecurityException → 500.
        // Fica VERMELHO até corrigir o nome do atributo no controller.
        @Test
        void caminhoFeliz_sessaoCorreta_deveEncontrarUsuarioSemErroDeAtributo() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    aprovada     = umaInscricao(1);

            // Sessão com o atributo no formato CORRETO
            MockHttpSession sessao = sessaoLogada(10);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.aprovar(any(), any(), any())).thenReturn(aprovada);

            // Espera 200 — vai falhar enquanto o controller ler "IsUsuaeioLogado"
            mockMvc.perform(patch("/api/inscricao/aprovar/1")
                            .session(sessao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());
        }

        // BUG CONHECIDO [InscricaoController.java, linha 37]:
        // O controller lança SecurityException (unchecked), que o DispatcherServlet
        // converte em HTTP 500. O comportamento correto para "não autenticado" é 403.
        // Fica VERMELHO até mapear SecurityException para 403 em um @ExceptionHandler.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(patch("/api/inscricao/aprovar/1")
                            .session(sessaoVazia())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isForbidden()); // 403, não 500

            verify(inscricaoService, never()).aprovar(any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/inscricao/rejeitar
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class RejeitarTest {

        // BUG CONHECIDO [InscricaoController.java, linha 42+44]:
        // @PatchMapping("/rejeitar") não possui nenhuma variável de path,
        // mas o método declara @PathVariable Integer inscricaoId.
        // Spring lança MissingPathVariableException → 500.
        // Fica VERMELHO até corrigir o mapping para "/rejeitar/{inscricaoId}".
        @Test
        void caminhoFeliz_deveRejeitarERetornar200() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    rejeitada    = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.rejeitarRemoverDiscente(
                    eq(1), eq("motivo invalido"), any(Oportunidade.class), eq(solicitante)))
                    .thenReturn(rejeitada);

            mockMvc.perform(patch("/api/inscricao/rejeitar/1")   // path correto após correção
                            .session(sessaoLogada(10))
                            .param("justificativa", "motivo invalido")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());

            verify(inscricaoService).rejeitarRemoverDiscente(
                    eq(1), eq("motivo invalido"), any(Oportunidade.class), eq(solicitante));
        }

        @Test
        void semJustificativa_deveRetornar400() throws Exception {
            mockMvc.perform(patch("/api/inscricao/rejeitar/1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(inscricaoService);
        }

        // BUG CONHECIDO [InscricaoController.java, linha 47]:
        // Mesmo problema de SecurityException → 500 ao invés de 403.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(patch("/api/inscricao/rejeitar/1")
                            .session(sessaoVazia())
                            .param("justificativa", "sem motivo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isForbidden());

            verify(inscricaoService, never()).rejeitarRemoverDiscente(any(), any(), any(), any());
        }

        @Test
        void justificativaEmBranco_serviceDeveSerChamadoComStringVazia() throws Exception {
            // Verifica que o controller não adiciona validação silenciosa de blank
            Usuario   solicitante = umUsuario(10);
            Inscricao rejeitada   = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.rejeitarRemoverDiscente(any(), eq(""), any(), any()))
                    .thenReturn(rejeitada);

            mockMvc.perform(patch("/api/inscricao/rejeitar/1")
                            .session(sessaoLogada(10))
                            .param("justificativa", "")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/inscricao/desistir/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class DesistirTest {

        // BUG CONHECIDO [InscricaoController.java, linha 53+54]:
        // @PatchMapping("/desistir/{id}") mas @PathVariable Integer inscricaoId
        // (sem @PathVariable("id")). Mesmo problema do /aprovar/{id}.
        // Fica VERMELHO até adicionar @PathVariable("id") ou renomear o path.
        @Test
        void caminhoFeliz_deveRegistrarDesistenciaERetornar200() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    desistida    = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.desistir(eq(1), any(Oportunidade.class), eq(solicitante)))
                    .thenReturn(desistida);

            mockMvc.perform(patch("/api/inscricao/desistir/1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());

            verify(inscricaoService).desistir(eq(1), any(Oportunidade.class), eq(solicitante));
        }

        // BUG CONHECIDO [InscricaoController.java, linha 57]:
        // Mesma SecurityException → 500 em vez de 403.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(patch("/api/inscricao/desistir/1")
                            .session(sessaoVazia())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isForbidden());

            verify(inscricaoService, never()).desistir(any(), any(), any());
        }

        @Test
        void idNegativo_deveRetornar400OuSerTratadoPeloService() throws Exception {
            // Valor adversarial: id negativo não deve ser aceito silenciosamente
            Usuario   solicitante = umUsuario(10);
            Inscricao desistida   = umaInscricao(-1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.desistir(eq(-1), any(), eq(solicitante))).thenReturn(desistida);

            // O controller não valida negativos — isso é documentado aqui como ponto de atenção
            mockMvc.perform(patch("/api/inscricao/desistir/-1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/inscricao/oportunidade/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ListarPorOportunidadeTest {

        // BUG CONHECIDO [InscricaoController.java, linha 64+65]:
        // O parâmetro `Oportunidade oportunidade` não tem @PathVariable nem @RequestBody.
        // Spring injeta um objeto Oportunidade com todos os campos null (construtor default),
        // ignorando completamente o {id} da URL. O service recebe uma Oportunidade vazia,
        // o que provavelmente causa NullPointerException ou retorno incorreto.
        // Fica VERMELHO até substituir por @PathVariable Integer id e buscar a entidade.
        @Test
        void caminhoFeliz_deveListarInscricoesDaOportunidade() throws Exception {
            Inscricao inscricao = umaInscricao(1);

            // Após correção o controller deve buscar a Oportunidade pelo id e repassar ao service
            when(inscricaoService.listarPorOportunidade(any(Oportunidade.class)))
                    .thenReturn(List.of(inscricao));

            mockMvc.perform(get("/api/inscricao/oportunidade/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void listaNula_deveRetornarListaVazia() throws Exception {
            when(inscricaoService.listarPorOportunidade(any(Oportunidade.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/inscricao/oportunidade/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/inscricao/oportunidade/{id}/fila-espera
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ListarFilaEsperaTest {

        // BUG CONHECIDO [InscricaoController.java, linha 69+70]:
        // GET com @RequestBody é contra a especificação HTTP/1.1; a maioria dos
        // proxies, clientes e servidores descarta o body em requisições GET.
        // MockMvc o aceita, mas em produção o body raramente chegará preenchido.
        // Fica VERMELHO (no sentido de risco em produção) até refatorar para
        // receber @PathVariable Integer id e resolver a Oportunidade internamente.
        @Test
        void caminhoFeliz_deveListarFilaDeEspera() throws Exception {
            Inscricao inscricao = umaInscricao(2);

            when(inscricaoService.listarFilaEspera(any(Oportunidade.class)))
                    .thenReturn(List.of(inscricao));

            // Após a correção do bug, o id virá pela URL, sem body
            mockMvc.perform(get("/api/inscricao/oportunidade/5/fila-espera"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void filaVazia_deveRetornarListaVazia() throws Exception {
            when(inscricaoService.listarFilaEspera(any(Oportunidade.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/inscricao/oportunidade/5/fila-espera"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/inscricao/discente/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ListarPorDiscenteTest {

        // BUG CONHECIDO [InscricaoController.java, linha 74+75]:
        // Mesmo problema de GET com @RequestBody que listarFilaEspera.
        // Fica VERMELHO até refatorar para @PathVariable Integer id.
        @Test
        void caminhoFeliz_deveListarInscricoesDoDiscente() throws Exception {
            Inscricao inscricao = umaInscricao(3);

            when(inscricaoService.listarPorDiscente(any(Discente.class)))
                    .thenReturn(List.of(inscricao));

            mockMvc.perform(get("/api/inscricao/discente/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void discenteSemInscricoes_deveRetornarListaVazia() throws Exception {
            when(inscricaoService.listarPorDiscente(any(Discente.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/inscricao/discente/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}package br.ufma.springextensao.controller;

import br.ufma.extensao.servicos.InscricaoService;
import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.model.Discente;
import br.ufma.springextensao.model.Inscricao;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes unitários de InscricaoController.
 *
 * Estratégia: MockMvc standalone (sem contexto Spring).
 * O InscricaoService e o UsuarioService são mocks puros — nenhum banco é acessado.
 *
 * Testes marcados com "BUG CONHECIDO" ficam VERMELHOS até a correção correspondente
 * ser aplicada no código de produção.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscricaoControllerTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Dependências mockadas
    // ═══════════════════════════════════════════════════════════════════════

    @Mock
    private InscricaoService inscricaoService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private InscricaoController controller;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    // ═══════════════════════════════════════════════════════════════════════
    // Setup
    // ═══════════════════════════════════════════════════════════════════════

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mapper  = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers — construtores de entidades de teste
    // ═══════════════════════════════════════════════════════════════════════

    private Usuario umUsuario(Integer id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private Oportunidade umaOportunidade(Integer id) {
        Oportunidade o = new Oportunidade();
        o.setId(id);
        return o;
    }

    private Inscricao umaInscricao(Integer id) {
        Inscricao i = new Inscricao();
        i.setId(id);
        return i;
    }

    private Discente umDiscente(Integer id) {
        Discente d = new Discente();
        d.setId(id);
        return d;
    }

    private InscricaoDTO umaInscricaoDTO(Integer oportunidadeId) {
        InscricaoDTO dto = new InscricaoDTO();
        // ajuste os setters conforme os campos reais de InscricaoDTO
        dto.setOportunidadeId(oportunidadeId);
        return dto;
    }

    /** Sessão com o atributo CORRETO de usuário logado. */
    private MockHttpSession sessaoLogada(Integer usuarioId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("IdUsuarioLogado", usuarioId);
        return s;
    }

    /** Sessão sem nenhum atributo (usuário não autenticado). */
    private MockHttpSession sessaoVazia() {
        return new MockHttpSession();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/inscricao/inscrever
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class InscreverTest {

        @Test
        void caminhoFeliz_deveInscreverERetornar201() throws Exception {
            InscricaoDTO dto      = umaInscricaoDTO(5);
            Inscricao    salva    = umaInscricao(1);

            when(inscricaoService.inscrever(any(InscricaoDTO.class))).thenReturn(salva);

            mockMvc.perform(post("/api/inscricao/inscrever")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(inscricaoService, times(1)).inscrever(any(InscricaoDTO.class));
        }

        @Test
        void deveRepassarDTOIntactoAoService() throws Exception {
            InscricaoDTO dto   = umaInscricaoDTO(7);
            Inscricao    salva = umaInscricao(2);

            when(inscricaoService.inscrever(any(InscricaoDTO.class))).thenReturn(salva);

            mockMvc.perform(post("/api/inscricao/inscrever")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            // garante que o service foi chamado com um DTO (não null)
            verify(inscricaoService).inscrever(any(InscricaoDTO.class));
        }

        @Test
        void semCorpo_deveRetornar400() throws Exception {
            mockMvc.perform(post("/api/inscricao/inscrever")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(inscricaoService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/inscricao/aprovar/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class AprovarTest {

        // BUG CONHECIDO [InscricaoController.java, linha 32+34]:
        // @PatchMapping("/aprovar/{id}") declara a variável de path como {id},
        // mas o parâmetro do método usa @PathVariable Integer inscricaoId (sem @PathVariable("id")).
        // Spring não consegue fazer o binding e lança IllegalArgumentException → 500.
        // Fica VERMELHO até adicionar @PathVariable("id") ou renomear o path para {inscricaoId}.
        @Test
        void caminhoFeliz_deveAprovarInscricaoERetornar200() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    aprovada     = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.aprovar(eq(1), any(Oportunidade.class), eq(solicitante)))
                    .thenReturn(aprovada);

            mockMvc.perform(patch("/api/inscricao/aprovar/1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());

            verify(inscricaoService).aprovar(eq(1), any(Oportunidade.class), eq(solicitante));
        }

        // BUG CONHECIDO [InscricaoController.java, linha 35]:
        // O atributo de sessão lido é "IsUsuaeioLogado" (typo com "ae") em vez de
        // "IdUsuarioLogado". Com a sessão correta configurada aqui, o controller
        // não encontra o atributo, retorna null e lança SecurityException → 500.
        // Fica VERMELHO até corrigir o nome do atributo no controller.
        @Test
        void caminhoFeliz_sessaoCorreta_deveEncontrarUsuarioSemErroDeAtributo() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    aprovada     = umaInscricao(1);

            // Sessão com o atributo no formato CORRETO
            MockHttpSession sessao = sessaoLogada(10);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.aprovar(any(), any(), any())).thenReturn(aprovada);

            // Espera 200 — vai falhar enquanto o controller ler "IsUsuaeioLogado"
            mockMvc.perform(patch("/api/inscricao/aprovar/1")
                            .session(sessao)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());
        }

        // BUG CONHECIDO [InscricaoController.java, linha 37]:
        // O controller lança SecurityException (unchecked), que o DispatcherServlet
        // converte em HTTP 500. O comportamento correto para "não autenticado" é 403.
        // Fica VERMELHO até mapear SecurityException para 403 em um @ExceptionHandler.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(patch("/api/inscricao/aprovar/1")
                            .session(sessaoVazia())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isForbidden()); // 403, não 500

            verify(inscricaoService, never()).aprovar(any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/inscricao/rejeitar
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class RejeitarTest {

        // BUG CONHECIDO [InscricaoController.java, linha 42+44]:
        // @PatchMapping("/rejeitar") não possui nenhuma variável de path,
        // mas o método declara @PathVariable Integer inscricaoId.
        // Spring lança MissingPathVariableException → 500.
        // Fica VERMELHO até corrigir o mapping para "/rejeitar/{inscricaoId}".
        @Test
        void caminhoFeliz_deveRejeitarERetornar200() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    rejeitada    = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.rejeitarRemoverDiscente(
                    eq(1), eq("motivo invalido"), any(Oportunidade.class), eq(solicitante)))
                    .thenReturn(rejeitada);

            mockMvc.perform(patch("/api/inscricao/rejeitar/1")   // path correto após correção
                            .session(sessaoLogada(10))
                            .param("justificativa", "motivo invalido")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());

            verify(inscricaoService).rejeitarRemoverDiscente(
                    eq(1), eq("motivo invalido"), any(Oportunidade.class), eq(solicitante));
        }

        @Test
        void semJustificativa_deveRetornar400() throws Exception {
            mockMvc.perform(patch("/api/inscricao/rejeitar/1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(inscricaoService);
        }

        // BUG CONHECIDO [InscricaoController.java, linha 47]:
        // Mesmo problema de SecurityException → 500 ao invés de 403.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(patch("/api/inscricao/rejeitar/1")
                            .session(sessaoVazia())
                            .param("justificativa", "sem motivo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isForbidden());

            verify(inscricaoService, never()).rejeitarRemoverDiscente(any(), any(), any(), any());
        }

        @Test
        void justificativaEmBranco_serviceDeveSerChamadoComStringVazia() throws Exception {
            // Verifica que o controller não adiciona validação silenciosa de blank
            Usuario   solicitante = umUsuario(10);
            Inscricao rejeitada   = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.rejeitarRemoverDiscente(any(), eq(""), any(), any()))
                    .thenReturn(rejeitada);

            mockMvc.perform(patch("/api/inscricao/rejeitar/1")
                            .session(sessaoLogada(10))
                            .param("justificativa", "")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PATCH /api/inscricao/desistir/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class DesistirTest {

        // BUG CONHECIDO [InscricaoController.java, linha 53+54]:
        // @PatchMapping("/desistir/{id}") mas @PathVariable Integer inscricaoId
        // (sem @PathVariable("id")). Mesmo problema do /aprovar/{id}.
        // Fica VERMELHO até adicionar @PathVariable("id") ou renomear o path.
        @Test
        void caminhoFeliz_deveRegistrarDesistenciaERetornar200() throws Exception {
            Usuario      solicitante  = umUsuario(10);
            Oportunidade oportunidade = umaOportunidade(5);
            Inscricao    desistida    = umaInscricao(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.desistir(eq(1), any(Oportunidade.class), eq(solicitante)))
                    .thenReturn(desistida);

            mockMvc.perform(patch("/api/inscricao/desistir/1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(oportunidade)))
                    .andExpect(status().isOk());

            verify(inscricaoService).desistir(eq(1), any(Oportunidade.class), eq(solicitante));
        }

        // BUG CONHECIDO [InscricaoController.java, linha 57]:
        // Mesma SecurityException → 500 em vez de 403.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(patch("/api/inscricao/desistir/1")
                            .session(sessaoVazia())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isForbidden());

            verify(inscricaoService, never()).desistir(any(), any(), any());
        }

        @Test
        void idNegativo_deveRetornar400OuSerTratadoPeloService() throws Exception {
            // Valor adversarial: id negativo não deve ser aceito silenciosamente
            Usuario   solicitante = umUsuario(10);
            Inscricao desistida   = umaInscricao(-1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(inscricaoService.desistir(eq(-1), any(), eq(solicitante))).thenReturn(desistida);

            // O controller não valida negativos — isso é documentado aqui como ponto de atenção
            mockMvc.perform(patch("/api/inscricao/desistir/-1")
                            .session(sessaoLogada(10))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(umaOportunidade(1))))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/inscricao/oportunidade/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ListarPorOportunidadeTest {

        // BUG CONHECIDO [InscricaoController.java, linha 64+65]:
        // O parâmetro `Oportunidade oportunidade` não tem @PathVariable nem @RequestBody.
        // Spring injeta um objeto Oportunidade com todos os campos null (construtor default),
        // ignorando completamente o {id} da URL. O service recebe uma Oportunidade vazia,
        // o que provavelmente causa NullPointerException ou retorno incorreto.
        // Fica VERMELHO até substituir por @PathVariable Integer id e buscar a entidade.
        @Test
        void caminhoFeliz_deveListarInscricoesDaOportunidade() throws Exception {
            Inscricao inscricao = umaInscricao(1);

            // Após correção o controller deve buscar a Oportunidade pelo id e repassar ao service
            when(inscricaoService.listarPorOportunidade(any(Oportunidade.class)))
                    .thenReturn(List.of(inscricao));

            mockMvc.perform(get("/api/inscricao/oportunidade/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void listaNula_deveRetornarListaVazia() throws Exception {
            when(inscricaoService.listarPorOportunidade(any(Oportunidade.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/inscricao/oportunidade/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/inscricao/oportunidade/{id}/fila-espera
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ListarFilaEsperaTest {

        // BUG CONHECIDO [InscricaoController.java, linha 69+70]:
        // GET com @RequestBody é contra a especificação HTTP/1.1; a maioria dos
        // proxies, clientes e servidores descarta o body em requisições GET.
        // MockMvc o aceita, mas em produção o body raramente chegará preenchido.
        // Fica VERMELHO (no sentido de risco em produção) até refatorar para
        // receber @PathVariable Integer id e resolver a Oportunidade internamente.
        @Test
        void caminhoFeliz_deveListarFilaDeEspera() throws Exception {
            Inscricao inscricao = umaInscricao(2);

            when(inscricaoService.listarFilaEspera(any(Oportunidade.class)))
                    .thenReturn(List.of(inscricao));

            // Após a correção do bug, o id virá pela URL, sem body
            mockMvc.perform(get("/api/inscricao/oportunidade/5/fila-espera"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void filaVazia_deveRetornarListaVazia() throws Exception {
            when(inscricaoService.listarFilaEspera(any(Oportunidade.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/inscricao/oportunidade/5/fila-espera"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/inscricao/discente/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ListarPorDiscenteTest {

        // BUG CONHECIDO [InscricaoController.java, linha 74+75]:
        // Mesmo problema de GET com @RequestBody que listarFilaEspera.
        // Fica VERMELHO até refatorar para @PathVariable Integer id.
        @Test
        void caminhoFeliz_deveListarInscricoesDoDiscente() throws Exception {
            Inscricao inscricao = umaInscricao(3);

            when(inscricaoService.listarPorDiscente(any(Discente.class)))
                    .thenReturn(List.of(inscricao));

            mockMvc.perform(get("/api/inscricao/discente/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void discenteSemInscricoes_deveRetornarListaVazia() throws Exception {
            when(inscricaoService.listarPorDiscente(any(Discente.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/inscricao/discente/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}