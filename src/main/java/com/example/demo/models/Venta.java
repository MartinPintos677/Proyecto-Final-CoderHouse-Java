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

  // Utilizamos una lista de LineaVenta en lugar de una lista de productos
  @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
  private List<LineaVenta> lineas; // Utilizamos una lista de LineaVenta en lugar de una lista de productos

  @Column
  private double total;

  @Column(name = "cantidad_productos")
  private int cantidadProductos;

  @Column
  private LocalDateTime fechaObtenidaDelServicio;
}
