package com.example.demo.controllers;

import com.example.demo.models.Producto;
import com.example.demo.models.Venta;
import com.example.demo.repository.ProductoRepository;
import com.example.demo.repository.VentaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("productos")
public class ProductoController {

  @Autowired
  private ProductoRepository productoRepo;

  @Autowired
  private VentaRepository ventaRepo;

  @GetMapping
  public List<Producto> getProductos() {
    return productoRepo.findAll();
  }

  @PostMapping("alta")
  public ResponseEntity<String> post(@RequestBody Producto producto) {
    productoRepo.save(producto);
    return ResponseEntity.ok("Producto guardado");
  }

  // Método para actualizar nombre y stock
  @PutMapping("modificar/{id}")
  public ResponseEntity<String> modificarProducto(@PathVariable Long id, @RequestBody Producto producto) {
    if (productoRepo.existsById(id)) {
      Producto updateProducto = productoRepo.getOne(id);
      updateProducto.setNombre(producto.getNombre());
      updateProducto.setStock(producto.getStock());
      productoRepo.save(updateProducto);
      return ResponseEntity.ok("Producto modificado");
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Producto no encontrado");
    }
  }

  // Método para actualizar precio (solo si el producto no ha sido vendido)
  @PutMapping("modificarPrecio/{id}")
  @Transactional
  public ResponseEntity<String> modificarPrecio(@PathVariable Long id, @RequestBody Producto producto) {
    Optional<Producto> optionalProducto = productoRepo.findById(id);

    if (optionalProducto.isPresent()) {
      Producto updateProducto = optionalProducto.get();

      // Verificar si el producto ha sido vendido
      boolean productoVendido = ventaRepo.existsByProductosContains(updateProducto);

      if (productoVendido) {
        return ResponseEntity.badRequest().body("No se puede actualizar el precio de un producto vendido");
      }

      // Actualizar precio
      updateProducto.setPrecio(producto.getPrecio());
      productoRepo.save(updateProducto);
      return ResponseEntity.ok("Precio modificado");
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Producto no encontrado");
    }
  }

  @DeleteMapping("baja/{id}")
  public ResponseEntity<String> delete(@PathVariable Long id) {
    if (productoRepo.existsById(id)) {
      Producto deleteProducto = productoRepo.getOne(id);
      productoRepo.delete(deleteProducto);
      return ResponseEntity.ok("Producto eliminado");
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Producto no encontrado");
    }
  }
}
