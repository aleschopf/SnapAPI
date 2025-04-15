package com.tivit.snap_api.teste;

import com.tivit.snap_api.annotations.SnapResource;
import com.tivit.snap_api.enums.Endpoint;
import jakarta.persistence.*;
import lombok.Data;

@SnapResource(
        path = "/produtos",
        expose = {Endpoint.GET_ALL, Endpoint.CREATE, Endpoint.GET_BY_ID, Endpoint.EDIT},
        searchableFields = {"nome", "categoria.nome"}
)
@Entity
@Data
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nome", column = @Column(name = "categoria_nome"))
    })
    private Categoria categoria;
}
