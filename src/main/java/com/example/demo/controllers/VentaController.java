package com.example.demo.controllers;

import com.example.demo.models.Cliente;
import com.example.demo.models.Producto;
import com.example.demo.models.Venta;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.ProductoRepository;
import com.example.demo.repository.VentaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("ventas")
public class VentaController {

  @Autowired
  private VentaRepository ventaRepo;

  @Autowired
  private ClienteRepository clienteRepo;

  @Autowired
  private ProductoRepository productoRepo;

  // En el método obtenerVentas del controlador de VentaController
  @GetMapping
  public List<Map<String, Object>> obtenerVentas() {
    List<Venta> ventas = ventaRepo.findAll();
    List<Map<String, Object>> ventasConDetalles = new ArrayList<>();

    for (Venta venta : ventas) {
      Map<String, Object> ventaConDetalles = new HashMap<>();
      ventaConDetalles.put("id", venta.getId());
      ventaConDetalles.put("cliente", getClienteMap(venta.getCliente()));
      ventaConDetalles.put("total", venta.getTotal());
      ventaConDetalles.put("fechaCreacion", venta.getFechaCreacion());
      ventaConDetalles.put("cantidadProductos", venta.getCantidadProductos());

      // Utilizar detallesProductos en lugar de la lista completa de productos
      String detallesProductos = venta.getDetallesProductos();
      ventaConDetalles.put("detallesProductos", detallesProductos);

      ventasConDetalles.add(ventaConDetalles);
    }

    return ventasConDetalles;
  }

  private Map<String, Object> getClienteMap(Cliente cliente) {
    Map<String, Object> clienteMap = new HashMap<>();
    clienteMap.put("id", cliente.getId());
    clienteMap.put("nombre", cliente.getNombre());
    clienteMap.put("email", cliente.getEmail());
    return clienteMap;
  }

  @PostMapping("alta")
  public ResponseEntity<Object> crearVenta(@RequestBody Venta venta) {
    Long clienteId = venta.getCliente().getId();
    Optional<Cliente> clienteOptional = clienteRepo.findById(clienteId);
    if (clienteOptional.isEmpty()) {
      return ResponseEntity.badRequest().body("El cliente con ID " + clienteId + " no existe");
    }

    Cliente cliente = clienteOptional.get();
    venta.setCliente(cliente);

    List<Producto> productosVendidos = venta.getProductos();
    boolean stockValido = true;

    for (Producto producto : productosVendidos) {
      Long productoId = producto.getId();
      Optional<Producto> productoOptional = productoRepo.findById(productoId);

      if (productoOptional.isEmpty()) {
        stockValido = false;
        System.out.println("El producto con ID " + productoId + " no existe");
        break;
      }

      Producto productoEnBD = productoOptional.get();
      int nuevoStock = productoEnBD.getStock() - 1;

      if (nuevoStock < 0) {
        stockValido = false;
        System.out.println("No hay suficiente stock para el producto con ID " + productoId);
        break;
      }

      // Cargar los detalles completos del producto
      producto.setNombre(productoEnBD.getNombre());
      producto.setPrecio(productoEnBD.getPrecio());
      producto.setStock(productoEnBD.getStock());
    }

    if (!stockValido) {
      return ResponseEntity.badRequest().body("Error en la venta, algunos productos no son válidos");
    }

    for (Producto producto : productosVendidos) {
      Long productoId = producto.getId();
      Producto productoEnBD = productoRepo.getOne(productoId);
      int nuevoStock = productoEnBD.getStock() - 1;
      productoEnBD.setStock(nuevoStock);
      productoRepo.save(productoEnBD);
    }

    BigDecimal totalVenta = calcularTotalVenta(productosVendidos);
    venta.setTotal(totalVenta.doubleValue());

    int cantidadProductosVendidos = productosVendidos.size();
    venta.setCantidadProductos(cantidadProductosVendidos);

    venta.setFechaCreacion(LocalDateTime.now());

    mostrarComprobanteYActualizarStock(venta);

    Map<String, Object> respuesta = construirRespuesta(venta);

    ObjectMapper objectMapper = new ObjectMapper();
    try {
      List<Map<String, Object>> detallesProductosList = new ArrayList<>();
      for (Producto producto : productosVendidos) {
        Map<String, Object> productoMap = new HashMap<>();
        productoMap.put("Nombre", producto.getNombre());
        productoMap.put("Precio", producto.getPrecio());
        productoMap.put("Stock", producto.getStock());
        detallesProductosList.add(productoMap);
      }
      String detallesProductosJson = objectMapper.writeValueAsString(detallesProductosList);
      venta.setDetallesProductos(detallesProductosJson);
    } catch (JsonProcessingException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al convertir productos a JSON");
    }

    ventaRepo.save(venta);

    return ResponseEntity.ok(respuesta);
  }

  @DeleteMapping("baja/{id}")
  public ResponseEntity<String> eliminarVenta(@PathVariable Long id) {
    if (ventaRepo.existsById(id)) {
      Venta venta = ventaRepo.getOne(id);

      List<Producto> productosVendidos = venta.getProductos();
      for (Producto producto : productosVendidos) {
        Producto productoEnBD = productoRepo.getOne(producto.getId());
        int nuevoStock = productoEnBD.getStock() + 1;
        productoEnBD.setStock(nuevoStock);
        productoRepo.save(productoEnBD);
      }

      ventaRepo.delete(venta);

      return ResponseEntity.ok("Venta eliminada");
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Venta no encontrada");
    }
  }

  private BigDecimal calcularTotalVenta(List<Producto> productos) {
    BigDecimal totalVenta = BigDecimal.ZERO;
    for (Producto producto : productos) {
      Producto productoEnBD = productoRepo.getOne(producto.getId());
      totalVenta = totalVenta.add(BigDecimal.valueOf(productoEnBD.getPrecio()));
    }
    return totalVenta;
  }

  private void mostrarComprobanteYActualizarStock(Venta venta) {
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

    List<Producto> stockActualizado = productoRepo.findAll();
    System.out.println("Stock Actualizado después de la venta:");
    for (Producto producto : stockActualizado) {
      System.out.println("Producto: " + producto.getNombre() + ", Stock: " + producto.getStock());
    }
  }

  private Map<String, Object> construirRespuesta(Venta venta) {
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

    return respuesta;
  }
}
