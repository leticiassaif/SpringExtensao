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
    public Oportunidade criaOportunidade(@RequestBody OportunidadeDTO dto, @RequestBody Usuario solicitante) {
        return service.criaOportunidade(dto, solicitante);
    }

    @PostMapping("/publicar/{id}")
    public Oportunidade publicarOportunidade(@PathVariable Integer id, @RequestBody Usuario solicitante) {
        return service.publicarOportunidade(id, solicitante);
    }

    @PostMapping("/aprovar/{id}")
    public Oportunidade aprovarOportunidade(@PathVariable Integer id, @RequestBody Usuario solicitante) {
        return service.aprovarOportunidade(id, solicitante);
    }

    @PostMapping("/iniciar/{id}")
    public Oportunidade iniciarOportunidade(@PathVariable Integer id, @RequestBody Usuario solicitante) {
        return service.iniciarOportunidade(id, solicitante);
    }

    @PostMapping("/encerrar/{id}")
    public Oportunidade encerrarOportunidade(@PathVariable Integer id, @RequestBody Usuario solicitante) {
        return service.encerrarOportunidade(id, solicitante);
    }

    @PostMapping
    public Oportunidade cancelarOportunidade(@PathVariable Integer id, @RequestBody Usuario solicitante) {
        return service.cancelarOportunidade(id, solicitante);
    }

    @GetMapping
    public List <Oportunidade> listarOportunidades() {
        return service.listarOportunidades();
    }

}
