package com.citt.controller;

import com.citt.persistence.entity.Producto;
import com.citt.persistence.repository.ProductoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/v1/productos")
@Tag(name = "Producto", description = "Controlador para consultar productos")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping
    @Operation(summary = "Obtener todos los productos")
    public ResponseEntity<List<Producto>> getProductos() {
        return ResponseEntity.ok(productoRepository.findAll());
    }

    @GetMapping("/{idProducto}")
    @Operation(summary = "Obtener un producto por ID")
    public ResponseEntity<Producto> obtenerProducto(@PathVariable Long idProducto) {
        return productoRepository.findById(idProducto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
