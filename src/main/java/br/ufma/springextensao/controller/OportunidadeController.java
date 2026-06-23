package br.ufma.springextensao.controller;

import br.ufma.springextensao.controller.dtos.OportunidadeDTO;
import br.ufma.springextensao.model.Oportunidade;
import br.ufma.springextensao.model.Usuario;
import br.ufma.springextensao.repository.OportunidadeRepo;
import br.ufma.springextensao.service.OportunidadeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/oportunidade")
public class OportunidadeController {

    @Autowired
    private OportunidadeService service;

    @PostMapping
    public Oportunidade criaOportunidade(@RequestBody OportunidadeDTO dto) {
        return service.criaOportunidade(dto);
    }

    @PostMapping("/publicar/{id}")
    public Oportunidade publicarOportunidade(@PathVariable Integer id, @RequestBody Usuario solicitante) {
        return service.publicarOportunidade(id, solicitante);
    }

    @PostMapping("/aprovar/{id}")
    public Oportunidade aprovarOportunidade(@PathVariable Integer id, @RequestBody Usuario solicitante) {
        return service.publicarOportunidade(id, solicitante);
    }

    @GetMapping("/oportunidade")
    public List<Oportunidade> listarOportunidades() {
        return service.listarOportunidades();
    }

}
