package com.example.demo.controllers;

import com.example.demo.models.Cliente;
import com.example.demo.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClienteController {

    @Autowired
    private ClienteRepository repo;

    @GetMapping
    public String index() {
        return "Conectado";
    }

    @GetMapping("clientes")
    public List<Cliente> getClientes() {
        return repo.findAll();
    }

    @PostMapping("alta")
    public String post(@RequestBody Cliente cliente) {
        repo.save(cliente);
        return "Guardado";
    }

    @PutMapping("modificar/{id}")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody Cliente cliente) {
        if (repo.existsById(id)) {
            Cliente updateCliente = repo.getOne(id);
            updateCliente.setNombre(cliente.getNombre());
            updateCliente.setEmail(cliente.getEmail());
            repo.save(updateCliente);
            return ResponseEntity.ok("Modificado");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cliente no encontrado");
        }
    }

    @DeleteMapping("baja/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        if (repo.existsById(id)) {
            Cliente deleteCliente = repo.getOne(id);
            repo.delete(deleteCliente);
            return ResponseEntity.ok("Eliminado");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Cliente no encontrado");
        }
    }
}
