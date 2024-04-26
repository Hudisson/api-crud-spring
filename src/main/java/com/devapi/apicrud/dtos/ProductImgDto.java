package com.devapi.apicrud.dtos;

//import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;

public record ProductImgDto( @NotBlank String pathImg) {
    
}
