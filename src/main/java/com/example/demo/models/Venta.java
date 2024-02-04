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

  @ManyToMany
  @JoinTable(name = "venta_producto", joinColumns = @JoinColumn(name = "venta_id"), inverseJoinColumns = @JoinColumn(name = "producto_id"))
  private List<Producto> productos;

  @Column
  private double total;

  @Column
  private LocalDateTime fechaCreacion;

  @Column(name = "cantidad_productos")
  private int cantidadProductos;
}
