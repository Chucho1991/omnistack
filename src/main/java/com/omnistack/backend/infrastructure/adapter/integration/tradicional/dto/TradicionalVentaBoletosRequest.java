package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradicionalVentaBoletosRequest {
    String userName;
    String token;
    Integer medioId;
    String reservaId;
    String cliente;
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
