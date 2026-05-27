package com.citt.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "despachos")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Despacho {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_despacho")
    private Long idDespacho;
    //@NotNull(message = "Fecha de despacho es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  // Especifica el formato de fecha
    @Column(name = "fecha_despacho")
    private LocalDate fechaDespacho;
    @Column(name = "patente_camion")
    private String patenteCamion;
    @Column(name = "intento")
    private int intento;
    @Column(name = "id_compra")
    private Long idCompra;
    //@NotBlank(message = "La dirección es obligatoria")
    @Column(name = "direccion_compra")
    private String direccionCompra;
    @Column(name = "valor_compra")
    private Long valorCompra;
    @Column(name = "entregado")
    private boolean despachado = false;
}
