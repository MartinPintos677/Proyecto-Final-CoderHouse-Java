package com.example.demo.controllers;

import com.example.demo.models.Cliente;
import com.example.demo.models.LineaVenta;
import com.example.demo.models.Producto;
import com.example.demo.models.Venta;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.ProductoRepository;
import com.example.demo.repository.VentaRepository;
import com.example.demo.repository.LineaVentaRepository;

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
        lineaMap.put("precioUnidad", linea.getProducto().getPrecio());
        detallesProductosMap.add(lineaMap);

        // Sumar la cantidad de productos vendidos en esta línea de venta
        cantidadTotalProductos += linea.getCantidad();
      }

      ventaConDetalles.put("cantidadProductos", cantidadTotalProductos);
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

      int cantidadTotalProductos = 0;

      List<Map<String, Object>> detallesProductosMap = new ArrayList<>();
      for (LineaVenta linea : venta.getLineas()) {
        Map<String, Object> lineaMap = new HashMap<>();
        lineaMap.put("producto", linea.getProducto().getNombre());
        lineaMap.put("cantidad", linea.getCantidad());
        lineaMap.put("precioUnitario", linea.getPrecioUnitario());
        detallesProductosMap.add(lineaMap);

        // Sumar la cantidad de productos vendidos en esta línea de venta
        cantidadTotalProductos += linea.getCantidad();
      }

      ventaConDetalles.put("cantidadProductos", cantidadTotalProductos);
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
    Map<String, Object> productosVendidos = new HashMap<>(); // Para almacenar los productos vendidos y sus detalles

    BigDecimal totalVenta = BigDecimal.ZERO;
    int cantidadProductos = 0;

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
      lineaVenta.setPrecioUnitario(producto.getPrecio());
      lineas.add(lineaVenta);

      // Agregar el producto y sus detalles al mapa de productos vendidos
      Map<String, Object> detalleProducto = new HashMap<>();
      detalleProducto.put("cantidad", lineaRequest.getCantidad());
      detalleProducto.put("precioUnitario", producto.getPrecio());
      productosVendidos.put(producto.getNombre(), detalleProducto);

      // Calcular el subtotal de esta línea y sumarlo al total de la venta
      BigDecimal subtotal = BigDecimal.valueOf(producto.getPrecio())
          .multiply(BigDecimal.valueOf(lineaRequest.getCantidad()));
      totalVenta = totalVenta.add(subtotal);

      // Incrementar la cantidad total de productos vendidos
      cantidadProductos += lineaRequest.getCantidad();
    }

    venta.setLineas(lineas);
    venta.setTotal(totalVenta.doubleValue()); // Establecer el total de la venta
    venta.setCantidadProductos(cantidadProductos); // Establecer la cantidad total de productos vendidos

    // Obtener la fecha del WebService externo y realizar otras operaciones
    // necesarias
    RestTemplate restTemplate = new RestTemplate();
    String url = "http://localhost:9090/fecha";
    String fechaObtenida = restTemplate.getForObject(url, String.class);
    LocalDateTime fechaObtenidaDelServicio = LocalDateTime.parse(fechaObtenida, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    venta.setFechaObtenidaDelServicio(fechaObtenidaDelServicio);

    ventaRepo.save(venta);

    // Obtener el stock actualizado de cada producto y mostrarlo
    List<Producto> productosActualizados = productoRepo.findAll();
    Map<String, Integer> stockActualizado = new HashMap<>();
    for (Producto p : productosActualizados) {
      stockActualizado.put(p.getNombre(), p.getStock());
    }

    // Construir el comprobante de la venta
    Map<String, Object> comprobanteVenta = new HashMap<>();
    comprobanteVenta.put("fecha", fechaObtenidaDelServicio);
    comprobanteVenta.put("cliente", cliente);
    comprobanteVenta.put("productosVendidos", productosVendidos);
    comprobanteVenta.put("totalVenta", totalVenta.doubleValue());
    comprobanteVenta.put("stockActualizado", stockActualizado);
    comprobanteVenta.put("cantidadProductos", cantidadProductos);

    return ResponseEntity.ok(comprobanteVenta);
  }
}