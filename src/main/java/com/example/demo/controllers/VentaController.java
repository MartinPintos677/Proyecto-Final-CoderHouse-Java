package com.example.demo.controllers;

import com.example.demo.models.Cliente;
import com.example.demo.models.Producto;
import com.example.demo.models.Venta;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.ProductoRepository;
import com.example.demo.repository.VentaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("ventas")
public class VentaController {

  @Autowired
  private VentaRepository ventaRepo;

  @Autowired
  private ClienteRepository clienteRepo;

  @Autowired
  private ProductoRepository productoRepo;

  // Método para obtener todas las ventas
  @GetMapping
  public List<Venta> obtenerVentas() {
    return ventaRepo.findAll();
  }

  @PostMapping("alta")
  public ResponseEntity<Object> crearVenta(@RequestBody Venta venta) {
    // Verificar si el cliente existe
    Long clienteId = venta.getCliente().getId();
    Optional<Cliente> clienteOptional = clienteRepo.findById(clienteId);
    if (clienteOptional.isEmpty()) {
      return ResponseEntity.badRequest().body("El cliente con ID " + clienteId + " no existe");
    }

    Cliente cliente = clienteOptional.get();
    venta.setCliente(cliente);

    // Verificar si los productos existen y actualizar el stock
    List<Producto> productosVendidos = venta.getProductos();
    boolean stockValido = true;

    // Verificar inicialmente el stock antes de comenzar el bucle
    for (Producto producto : productosVendidos) {
      Long productoId = producto.getId();
      Optional<Producto> productoOptional = productoRepo.findById(productoId);

      if (productoOptional.isEmpty()) {
        stockValido = false;
        System.out.println("El producto con ID " + productoId + " no existe");
        break;
      }

      Producto productoEnBD = productoOptional.get();
      int nuevoStock = productoEnBD.getStock() - 1; // Ejemplo: Restar uno al stock

      if (nuevoStock < 0) {
        stockValido = false;
        System.out.println("No hay suficiente stock para el producto con ID " + productoId);
        break;
      }
    }

    // Si el stock no es válido, no proceder con la venta
    if (!stockValido) {
      return ResponseEntity.badRequest().body("Error en la venta, algunos productos no son válidos");
    }

    // Decrementar el stock dentro del bucle solo si todo está en orden
    for (Producto producto : productosVendidos) {
      Long productoId = producto.getId();
      Producto productoEnBD = productoRepo.getOne(productoId);
      int nuevoStock = productoEnBD.getStock() - 1; // Ejemplo: Restar uno al stock
      productoEnBD.setStock(nuevoStock);
      productoRepo.save(productoEnBD);
    }

    // Calcular el total de la venta dinámicamente
    BigDecimal totalVenta = BigDecimal.ZERO;
    for (Producto producto : productosVendidos) {
      Producto productoEnBD = productoRepo.getOne(producto.getId());
      totalVenta = totalVenta.add(BigDecimal.valueOf(productoEnBD.getPrecio()));
    }
    venta.setTotal(totalVenta.doubleValue());

    // Calcular la cantidad de productos vendidos
    int cantidadProductosVendidos = productosVendidos.size();
    venta.setCantidadProductos(cantidadProductosVendidos);

    // Establecer la fecha de creación
    venta.setFechaCreacion(LocalDateTime.now());

    // Mostrar el comprobante de la venta
    System.out.println("\nComprobante de la venta:");
    System.out.println("Fecha: " + venta.getFechaCreacion());
    System.out.println("Cliente: " + venta.getCliente().getNombre());

    System.out.println("Productos vendidos:");
    for (Producto producto : venta.getProductos()) {
      Long productoId = producto.getId();
      Optional<Producto> productoOptional = productoRepo.findById(productoId);

      if (productoOptional.isPresent()) {
        Producto productoEnBD = productoOptional.get();
        System.out.println("  - Nombre: " + productoEnBD.getNombre());
        System.out.println("    Precio: " + productoEnBD.getPrecio());
      }
    }

    System.out.println("Total de la venta: " + venta.getTotal());

    // Al final de cada venta, mostrar el stock
    List<Producto> stockActualizado = productoRepo.findAll();
    System.out.println("Stock Actualizado después de la venta:");
    for (Producto producto : stockActualizado) {
      System.out.println("Producto: " + producto.getNombre() + ", Stock: " + producto.getStock());
    }

    // Mostrar el comprobante de la venta
    Map<String, Object> respuesta = new HashMap<>();
    respuesta.put("Fecha", venta.getFechaCreacion());
    respuesta.put("Cliente", venta.getCliente().getNombre());

    List<Map<String, Object>> productosVendidosMap = new ArrayList<>();
    for (Producto producto : venta.getProductos()) {
      Long productoId = producto.getId();
      Optional<Producto> productoOptional = productoRepo.findById(productoId);

      if (productoOptional.isPresent()) {
        Producto productoEnBD = productoOptional.get();
        Map<String, Object> productoMap = new HashMap<>();
        productoMap.put("Nombre", productoEnBD.getNombre());
        productoMap.put("Precio", productoEnBD.getPrecio());
        productoMap.put("Stock", productoEnBD.getStock());
        productosVendidosMap.add(productoMap);
      }
    }
    respuesta.put("Productos vendidos", productosVendidosMap);

    respuesta.put("Total de la venta", venta.getTotal());

    // Guardar la venta en la base de datos
    ventaRepo.save(venta);

    // Devolver una respuesta exitosa
    return ResponseEntity.ok(respuesta);
  }

  @DeleteMapping("baja/{id}")
  public ResponseEntity<String> eliminarVenta(@PathVariable Long id) {
    if (ventaRepo.existsById(id)) {
      Venta venta = ventaRepo.getOne(id);

      // Devolver el stock a los productos vendidos
      List<Producto> productosVendidos = venta.getProductos();
      for (Producto producto : productosVendidos) {
        Producto productoEnBD = productoRepo.getOne(producto.getId());
        int nuevoStock = productoEnBD.getStock() + 1; // Incrementar uno al stock
        productoEnBD.setStock(nuevoStock);
        productoRepo.save(productoEnBD);
      }

      // Eliminar la venta de la base de datos
      ventaRepo.delete(venta);

      return ResponseEntity.ok("Venta eliminada");
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Venta no encontrada");
    }
  }
}
