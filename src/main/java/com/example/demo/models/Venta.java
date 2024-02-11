package com.example.demo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import java.util.List;

@Getter
@Setter
@Entity
public class Venta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "venta_producto", joinColumns = @JoinColumn(name = "venta_id"), inverseJoinColumns = @JoinColumn(name = "producto_id"))
  private List<Producto> productos;

  @Column
  private double total;

  // En la entidad Venta, agregamos un nuevo campo para almacenar la fecha
  // obtenida del servicio REST
  @Column
  private LocalDateTime fechaObtenidaDelServicio;

  @Column(name = "cantidad_productos")
  private int cantidadProductos;

  @Column(columnDefinition = "TEXT")
  private String detallesProductos; // Almacena los detalles de los productos en formato JSON
}
