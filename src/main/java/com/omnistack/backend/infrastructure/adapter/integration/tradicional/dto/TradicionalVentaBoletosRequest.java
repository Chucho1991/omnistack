package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradicionalVentaBoletosRequest {
    String userName;
    String token;
    String medioId;
    String reservaId;
    String cliente;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    BigDecimal totalVenta;
    String numeroIdentificacion;
    String nombreComprador;
    String numeroCelularComprador;
    String correoElectronicoComprador;

    @JsonProperty("ListaOrdenCompra")
    List<OrdenCompra> listaOrdenCompra;

    List<JuegoEntry> listaJuegos;

    @Value
    @Builder
    public static class OrdenCompra {
        String ordenCompra;
        List<FormaCobro> listaFormaCobro;
    }

    @Value
    @Builder
    public static class FormaCobro {
        String formaCobro;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal total;
    }

    @Value
    @Builder
    public static class JuegoEntry {
        String juegoId;
        List<SorteoEntry> listaSorteos;
    }

    @Value
    @Builder
    public static class SorteoEntry {
        String sorteoId;
        String numero;
        Integer cantidadBoletos;
    }
}
