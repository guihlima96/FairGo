package br.fairgo;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/admin/validacao/MOTORISTA/{id}/conferencias-identidade")
class ConferenciaIdentidadeAdmin {
    private final ConferenciaIdentidade conferencias;

    ConferenciaIdentidadeAdmin(ConferenciaIdentidade conferencias) {
        this.conferencias = conferencias;
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public void registrar(@PathVariable UUID id, @Valid @RequestBody ConferenciaIdentidade.Pedido pedido,
                          Principal admin) {
        conferencias.registrar(id, pedido, admin.getName());
    }

    @GetMapping
    public List<ConferenciaIdentidade.Evento> listar(@PathVariable UUID id) {
        return conferencias.historico(id);
    }
}
