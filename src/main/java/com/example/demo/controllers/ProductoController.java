package com.example.demo.controllers;

import com.example.demo.models.Producto;
import com.example.demo.repository.ProductoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("productos")
public class ProductoController {

  @Autowired
  private ProductoRepository productoRepo;

  @GetMapping
  public List<Producto> getProductos() {
    return productoRepo.findAll();
  }

  @PostMapping("alta")
  public ResponseEntity<String> post(@RequestBody Producto producto) {
    productoRepo.save(producto);
    return ResponseEntity.ok("Producto guardado");
  }

  // Método para actualizar producto
  @PutMapping("modificar/{id}")
  public ResponseEntity<String> modificarProducto(@PathVariable Long id, @RequestBody Producto producto) {
    if (productoRepo.existsById(id)) {
      Producto updateProducto = productoRepo.getOne(id);
      updateProducto.setNombre(producto.getNombre());
      updateProducto.setStock(producto.getStock());
      updateProducto.setPrecio(producto.getPrecio());
      productoRepo.save(updateProducto);
      return ResponseEntity.ok("Producto modificado");
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Producto no encontrado");
    }
  }
}
