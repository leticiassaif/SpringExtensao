package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.service.OportunidadeService;
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
 * Testes unitários de OportunidadeController.
 *
 * Estratégia: MockMvc standalone (sem contexto Spring).
 * OportunidadeService e UsuarioService são mocks puros.
 *
 * Testes marcados com "BUG CONHECIDO" ficam VERMELHOS até a correção correspondente
 * ser aplicada no código de produção.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OportunidadeControllerTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Dependências mockadas
    // ═══════════════════════════════════════════════════════════════════════

    @Mock
    private OportunidadeService service;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private OportunidadeController controller;

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

    private OportunidadeDTO umDTO(String titulo) {
        OportunidadeDTO dto = new OportunidadeDTO();
        // ajuste os setters conforme os campos reais de OportunidadeDTO
        dto.setTitulo(titulo);
        return dto;
    }

    /** Sessão com usuário autenticado. */
    private MockHttpSession sessaoLogada(Integer usuarioId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("IdUsuarioLogado", usuarioId);
        return s;
    }

    /** Sessão sem nenhum atributo (não autenticado). */
    private MockHttpSession sessaoVazia() {
        return new MockHttpSession();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/oportunidade
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class CriaOportunidadeTest {

        // BUG CONHECIDO [OportunidadeController.java, linha 29]:
        // O método declara DOIS parâmetros com @RequestBody:
        //   @RequestBody OportunidadeDTO dto, @RequestBody Usuario solicitante
        // O Spring só consegue desserializar um único @RequestBody por handler.
        // O segundo parâmetro nunca é preenchido — Spring lança
        // HttpMessageNotReadableException ou o parâmetro chega null → NullPointerException.
        // Fica VERMELHO até remover o @RequestBody de Usuario e obter o solicitante
        // via HttpSession (padrão dos outros métodos) ou via @RequestHeader.
        @Test
        void caminhoFeliz_deveCriarOportunidadeERetornar200() throws Exception {
            OportunidadeDTO dto        = umDTO("Monitoria de Cálculo");
            Usuario         solicitante = umUsuario(10);
            Oportunidade    criada      = umaOportunidade(1);

            // Após correção: solicitante virá da sessão, não do body
            when(service.criaOportunidade(any(OportunidadeDTO.class), any(Usuario.class)))
                    .thenReturn(criada);

            mockMvc.perform(post("/api/oportunidade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())       // espera 200 (ou 201 após ajuste semântico)
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).criaOportunidade(any(OportunidadeDTO.class), any(Usuario.class));
        }

        @Test
        void semCorpoJson_deveRetornar400() throws Exception {
            mockMvc.perform(post("/api/oportunidade")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }

        @Test
        void tituloNulo_deveRetornar400() throws Exception {
            // Adversarial: DTO com título null deve ser rejeitado por @Valid / @NotNull
            // (a anotação ainda precisa ser adicionada ao DTO)
            OportunidadeDTO dto = umDTO(null);

            mockMvc.perform(post("/api/oportunidade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void tituloVazio_deveRetornar400() throws Exception {
            OportunidadeDTO dto = umDTO("");

            mockMvc.perform(post("/api/oportunidade")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/oportunidade/publicar/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class PublicarOportunidadeTest {

        @Test
        void caminhoFeliz_devePublicarERetornar200() throws Exception {
            Usuario      solicitante = umUsuario(10);
            Oportunidade publicada   = umaOportunidade(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(service.publicarOportunidade(eq(1), eq(solicitante))).thenReturn(publicada);

            mockMvc.perform(post("/api/oportunidade/publicar/1")
                            .session(sessaoLogada(10)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).publicarOportunidade(eq(1), eq(solicitante));
        }

        // BUG CONHECIDO [OportunidadeController.java, linha 37]:
        // O controller lança SecurityException (unchecked), que o Spring converte em
        // HTTP 500. O comportamento correto para usuário não autenticado é 403 Forbidden.
        // Fica VERMELHO até mapear SecurityException → 403 em um @ExceptionHandler global.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(post("/api/oportunidade/publicar/1")
                            .session(sessaoVazia()))
                    .andExpect(status().isForbidden()); // 403, não 500

            verify(service, never()).publicarOportunidade(any(), any());
        }

        @Test
        void idInexistente_serviceDevePropagarExcecao() throws Exception {
            Usuario solicitante = umUsuario(10);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(service.publicarOportunidade(eq(999), eq(solicitante)))
                    .thenThrow(new IllegalArgumentException("Oportunidade não encontrada"));

            mockMvc.perform(post("/api/oportunidade/publicar/999")
                            .session(sessaoLogada(10)))
                    .andExpect(status().isBadRequest()); // ou 404, conforme ExceptionHandler
        }

        @Test
        void idNegativo_serviceDevePropagarExcecao() throws Exception {
            // Adversarial: valores negativos não devem ser aceitos silenciosamente
            Usuario solicitante = umUsuario(10);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(service.publicarOportunidade(eq(-1), eq(solicitante)))
                    .thenThrow(new IllegalArgumentException("Id inválido"));

            mockMvc.perform(post("/api/oportunidade/publicar/-1")
                            .session(sessaoLogada(10)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST /api/oportunidade/aprovar/{id}
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class AprovarOportunidadeTest {

        @Test
        void caminhoFeliz_deveAprovarERetornar200() throws Exception {
            Usuario      solicitante = umUsuario(10);
            Oportunidade aprovada    = umaOportunidade(1);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(service.aprovarOportunidade(eq(1), eq(solicitante))).thenReturn(aprovada);

            mockMvc.perform(post("/api/oportunidade/aprovar/1")
                            .session(sessaoLogada(10)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(service).aprovarOportunidade(eq(1), eq(solicitante));
        }

        // BUG CONHECIDO [OportunidadeController.java, linha 46]:
        // SecurityException → 500 em vez de 403, mesmo problema de publicar.
        @Test
        void usuarioNaoLogado_deveRetornar403() throws Exception {
            when(usuarioService.buscarPorId(any())).thenReturn(null);

            mockMvc.perform(post("/api/oportunidade/aprovar/1")
                            .session(sessaoVazia()))
                    .andExpect(status().isForbidden());

            verify(service, never()).aprovarOportunidade(any(), any());
        }

        @Test
        void statusInvalido_serviceDevePropagarExcecao() throws Exception {
            // Ex.: aprovar uma oportunidade que já está ENCERRADA
            Usuario solicitante = umUsuario(10);

            when(usuarioService.buscarPorId(10)).thenReturn(solicitante);
            when(service.aprovarOportunidade(eq(2), eq(solicitante)))
                    .thenThrow(new IllegalStateException("Transição de status inválida"));

            mockMvc.perform(post("/api/oportunidade/aprovar/2")
                            .session(sessaoLogada(10)))
                    .andExpect(status().isConflict()); // 409, configurado no ExceptionHandler
        }

        @Test
        void usuarioSemPermissao_serviceDevePropagarExcecao() throws Exception {
            // Ex.: discente tenta aprovar (papel não autorizado)
            Usuario solicitante = umUsuario(20);

            when(usuarioService.buscarPorId(20)).thenReturn(solicitante);
            when(service.aprovarOportunidade(eq(1), eq(solicitante)))
                    .thenThrow(new SecurityException("Sem permissão para aprovar"));

            mockMvc.perform(post("/api/oportunidade/aprovar/1")
                            .session(sessaoLogada(20)))
                    .andExpect(status().isForbidden()); // 403
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GET /api/oportunidade/oportunidade  (atenção: caminho duplicado!)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    class ListarOportunidadesTest {

        // BUG CONHECIDO [OportunidadeController.java, linha 51]:
        // @GetMapping("/oportunidade") sob @RequestMapping("/api/oportunidade") gera
        // o path COMPLETO: /api/oportunidade/oportunidade.
        // A intenção provável é que o endpoint seja GET /api/oportunidade (raiz).
        // Fica VERMELHO enquanto o path correto for /api/oportunidade mas o mapping
        // responder apenas em /api/oportunidade/oportunidade.
        @Test
        void caminhoFeliz_deveListarOportunidades_noPathCorreto() throws Exception {
            Oportunidade o = umaOportunidade(1);
            when(service.listarOportunidades()).thenReturn(List.of(o));

            // Path CORRETO esperado após correção: GET /api/oportunidade
            mockMvc.perform(get("/api/oportunidade"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void listaVazia_deveRetornar200ComArrayVazio() throws Exception {
            when(service.listarOportunidades()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/oportunidade"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void pathDuplicado_naoDeveMaisExistir() throws Exception {
            // Confirma que após a correção, /api/oportunidade/oportunidade retorna 404
            // (o path duplicado não deve existir como rota pública)
            mockMvc.perform(get("/api/oportunidade/oportunidade"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void serviceRetornandoNull_deveRetornar500OuListaVazia() throws Exception {
            // Adversarial: service retorna null em vez de lista vazia
            when(service.listarOportunidades()).thenReturn(null);

            // O controller não trata null — Spring tentará serializar null como JSON "null"
            // ou lançará NullPointerException dependendo do serializer.
            // O correto seria o service nunca retornar null (deve retornar lista vazia).
            mockMvc.perform(get("/api/oportunidade"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()); // deve ser array, não null
        }
    }
}