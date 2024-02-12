package com.example.demo.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class LineaVenta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "venta_id")
  private Venta venta;

  @Column
  private int cantidad;

  @ManyToOne
  @JoinColumn(name = "producto_id")
  private Producto producto;

  // AGREGAMOS PRECIO ACÁ ??? CREO QUE SI
  @Column(name = "precio_unitario")
  private double precioUnitario;
}
