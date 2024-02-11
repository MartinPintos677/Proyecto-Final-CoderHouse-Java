package com.example.demo.repository;

import com.example.demo.models.Producto;
import com.example.demo.models.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<Venta, Long> {

}
