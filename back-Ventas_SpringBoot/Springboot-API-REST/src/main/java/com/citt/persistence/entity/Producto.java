package com.citt.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "productos")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "\"idProducto\"")
    private Long idProducto;

    @Column(name = "\"nombreProducto\"")
    private String nombreProducto;

    @Column(name = "\"descripcionProducto\"")
    private String descripcionProducto;

    @Column(name = "\"precioProducto\"")
    private int precioProducto;

    @Column(name = "\"stockProducto\"")
    private int stockProducto;
}
