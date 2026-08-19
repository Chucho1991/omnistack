package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Campo parametrico requerido por un servicio comercial.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Campo requerido por el servicio para una capacidad concreta")
public class BusinessLineInputFieldResponse {
    String id;
    String label;
    String type;
    String capability;
    boolean required;
    String group;
    String conditional;

    @Schema(description = "Restriccion de tamano segun type: STRING=max caracteres, INTEGER=max digitos, DOUBLE=cantidad de decimales. "
            + "Si groupLength no es null, length pasa a significar 'cantidad de grupos' en vez de 'cantidad de caracteres'. Null=sin restriccion.")
    Integer length;

    @Schema(description = "Patron de validacion del valor tal como lo escribe el cajero (texto crudo, antes de convertirlo "
            + "segun type). Aplica a cualquier type (STRING/INTEGER/DOUBLE). Null=sin regex, se valida solo por type/length. "
            + "IMPORTANTE: si groupLength es null, el regex valida el VALOR COMPLETO (ej. Loteria: length=5, regex=\"^[0-9]+$\" "
            + "sobre \"12345\"). Si groupLength NO es null, el regex valida CADA GRUPO por separado, no el valor concatenado "
            + "(ej. Pozo: length=4, groupLength=2, regex=\"^(?:0[1-9]|1[0-9]|2[0-5])$\" se aplica a cada uno de los 4 numeros "
            + "de \"22 22 22 01\", no a la cadena completa).")
    String regex;

    @Schema(description = "Caracteres por grupo, solo para campos de texto (type=STRING) compuestos por varios numeros "
            + "concatenados con espacio (ej: combinacion de loteria: length=4 grupos, groupLength=2 digitos c/u -> \"11 21 23 23\"). "
            + "No aplica a type=INTEGER/DOUBLE (un unico valor numerico no se agrupa) — en esos casos siempre es null. "
            + "Null=campo simple, no agrupado (length=cantidad de caracteres, ej. \"1234\"). "
            + "1=agrupado con 1 caracter por grupo, DISTINTO de null (ej. length=4, groupLength=1 -> \"1 2 3 4\", con espacios). "
            + "0 no es un valor valido (constraint CK_INPUT_FIELDS_GROUP_LENGTH en BD lo impide).")
    @JsonProperty("group_length")
    Integer groupLength;
}
