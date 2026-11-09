package com.botica.inteligente.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Datos del emisor que aparecen en los comprobantes (propiedades botica.empresa.*). */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "botica.empresa")
public class EmpresaProperties {

    private String nombre = "Botica Inteligente";
    private String ruc = "";
    private String direccion = "";
    private String telefono = "";
}
