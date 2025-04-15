package com.tivit.snap_api.teste;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Categoria {

    private String nome;
}
