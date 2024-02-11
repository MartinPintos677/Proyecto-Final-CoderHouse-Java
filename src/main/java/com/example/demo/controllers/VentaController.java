package com.example.demo.controllers;

import com.example.demo.models.Cliente;
import com.example.demo.models.LineaVenta;
import com.example.demo.models.Producto;
import com.example.demo.models.Venta;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.ProductoRepository;
import com.example.demo.repository.VentaRepository;
import com.example.demo.repository.LineaVentaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("ventas")
public class VentaController {

  @Autowired
  private VentaRepository ventaRepo;

  @Autowired
  private ClienteRepository clienteRepo;

  @Autowired
  private ProductoRepository productoRepo;

  @Autowired
  private LineaVentaRepository lineaVentaRepo;

  @GetMapping
  public List<Map<String, Object>> obtenerVentas() {
    List<Venta> ventas = ventaRepo.findAll();
    List<Map<String, Object>> ventasConDetalles = new ArrayList<>();

    for (Venta venta : ventas) {
      Map<String, Object> ventaConDetalles = new HashMap<>();
      ventaConDetalles.put("id", venta.getId());
      ventaConDetalles.put("cliente", getClienteMap(venta.getCliente()));
      ventaConDetalles.put("total", venta.getTotal());
      ventaConDetalles.put("fechaCreacion", venta.getFechaObtenidaDelServicio());

      int cantidadTotalProductos = 0; // Inicializar la cantidad total de productos vendidos

      List<Map<String, Object>> detallesProductosMap = new ArrayList<>();
      for (LineaVenta linea : venta.getLineas()) {
        Map<String, Object> lineaMap = new HashMap<>();
        lineaMap.put("producto", linea.getProducto().getNombre());
        lineaMap.put("cantidad", linea.getCantidad());
        detallesProductosMap.add(lineaMap);

        // Sumar la cantidad de productos vendidos en esta línea de venta
        cantidadTotalProductos += linea.getCantidad();
      }

      ventaConDetalles.put("cantidadProductos", cantidadTotalProductos); // Agregar la cantidad total de productos
                                                                         // vendidos
      ventaConDetalles.put("detallesProductos", detallesProductosMap);

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

  @GetMapping("{id}")
  public ResponseEntity<Object> obtenerVentaPorId(@PathVariable Long id) {
    Optional<Venta> ventaOptional = ventaRepo.findById(id);
    if (ventaOptional.isPresent()) {
      Venta venta = ventaOptional.get();
      Map<String, Object> ventaConDetalles = new HashMap<>();
      ventaConDetalles.put("id", venta.getId());
      ventaConDetalles.put("cliente", getClienteMap(venta.getCliente()));
      ventaConDetalles.put("total", venta.getTotal());
      ventaConDetalles.put("fechaCreacion", venta.getFechaObtenidaDelServicio());

      int cantidadTotalProductos = 0; // Inicializar la cantidad total de productos vendidos

      List<Map<String, Object>> detallesProductosMap = new ArrayList<>();
      for (LineaVenta linea : venta.getLineas()) {
        Map<String, Object> lineaMap = new HashMap<>();
        lineaMap.put("producto", linea.getProducto().getNombre());
        lineaMap.put("cantidad", linea.getCantidad());
        detallesProductosMap.add(lineaMap);

        // Sumar la cantidad de productos vendidos en esta línea de venta
        cantidadTotalProductos += linea.getCantidad();
      }

      ventaConDetalles.put("cantidadProductos", cantidadTotalProductos); // Agregar la cantidad total de productos
                                                                         // vendidos
      ventaConDetalles.put("detallesProductos", detallesProductosMap);

      return ResponseEntity.ok(ventaConDetalles);
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Venta no encontrada");
    }
  }

  @PostMapping("alta")
  public ResponseEntity<Object> crearVenta(@RequestBody Venta ventaRequest) {
    Venta venta = new Venta();
    Cliente cliente = clienteRepo.findById(ventaRequest.getCliente().getId())
        .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    venta.setCliente(cliente);

    List<LineaVenta> lineas = new ArrayList<>();
    int cantidadTotalProductos = 0;

    for (LineaVenta lineaRequest : ventaRequest.getLineas()) {
      LineaVenta lineaVenta = new LineaVenta();
      Producto producto = productoRepo.findById(lineaRequest.getProducto().getId())
          .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

      // Verificar si la cantidad solicitada supera el stock disponible
      if (producto.getStock() < lineaRequest.getCantidad()) {
        return ResponseEntity.badRequest().body("No hay suficiente stock para el producto: " + producto.getNombre());
      }

      // Actualizar el stock del producto
      int nuevoStock = producto.getStock() - lineaRequest.getCantidad();
      producto.setStock(nuevoStock);
      productoRepo.save(producto); // Guardar la actualización del stock

      lineaVenta.setProducto(producto);
      lineaVenta.setCantidad(lineaRequest.getCantidad());
      lineaVenta.setVenta(venta);
      lineas.add(lineaVenta);

      // Sumar la cantidad de productos vendidos
      cantidadTotalProductos += lineaRequest.getCantidad();
    }

    venta.setLineas(lineas);
    venta.setCantidadProductos(cantidadTotalProductos);

    BigDecimal totalVenta = calcularTotalVenta(lineas);
    venta.setTotal(totalVenta.doubleValue());

    // Obtener la fecha y realizar otras operaciones necesarias

    // Realiza una solicitud HTTP al servicio REST para obtener la fecha
    RestTemplate restTemplate = new RestTemplate();
    String url = "http://localhost:9090/fecha";
    String fechaObtenida = restTemplate.getForObject(url, String.class);

    // Parsea la fecha obtenida a LocalDateTime
    LocalDateTime fechaObtenidaDelServicio = LocalDateTime.parse(fechaObtenida, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    // Establece la fecha obtenida en la venta
    venta.setFechaObtenidaDelServicio(fechaObtenidaDelServicio);

    // Guardar la venta en el repositorio
    ventaRepo.save(venta);

    // Realizar otras operaciones necesarias

    return ResponseEntity.ok("Venta creada exitosamente");
  }

  private BigDecimal calcularTotalVenta(List<LineaVenta> lineas) {
    BigDecimal totalVenta = BigDecimal.ZERO;
    for (LineaVenta linea : lineas) {
      Producto producto = linea.getProducto();
      BigDecimal subtotal = BigDecimal.valueOf(producto.getPrecio()).multiply(BigDecimal.valueOf(linea.getCantidad()));
      totalVenta = totalVenta.add(subtotal);
    }
    return totalVenta;
  }

  @DeleteMapping("baja/{id}")
  public ResponseEntity<String> eliminarVenta(@PathVariable Long id) {
    if (ventaRepo.existsById(id)) {
      Venta venta = ventaRepo.getOne(id);

      for (LineaVenta linea : venta.getLineas()) {
        Producto producto = linea.getProducto();
        int nuevoStock = producto.getStock() + linea.getCantidad();
        producto.setStock(nuevoStock);
        productoRepo.save(producto);
      }

      ventaRepo.delete(venta);

      return ResponseEntity.ok("Venta eliminada");
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Venta no encontrada");
    }
  }
}
