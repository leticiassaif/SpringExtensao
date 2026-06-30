package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.InscricaoDTO;
import br.ufma.springextensao.model.Inscricao;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import br.ufma.extensao.servicos.InscricaoService;

import java.util.List;

@RestController
@RequestMapping("/api/inscricao")
public class InscricaoController {

    @Autowired
    private InscricaoService service;

    @PostMapping
    public inscrever(@RequestBody InscricaoDTO dto) {
        return service.inscrever(dto);

    }

    @PostMapping("/aprovar/{id}")
    public Inscricao aprovar(Integer inscricaoId, Oportunidade oportunidade, Usuario solicitante) {

    }

    @PostMapping("/rejeitar")
    public Inscricao rejeitarRemoverDiscente(Integer inscricaoId, String justificativa,
                                             Oportunidade oportunidade, Usuario solicitante) {

    }

    @PostMapping("/remover")
    public Inscricao desistir(Integer inscricaoId, Oportunidade oportunidade,
                              Usuario solicitante) {

    }

    @GetMapping("/inscricao")
    public List<Inscricao> listarPorOportunidade(Oportunidade oportunidade) {
        return service.listarPorOportunidade(oportunidade);
    }

}
