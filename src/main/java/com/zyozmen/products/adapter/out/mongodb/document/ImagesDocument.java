package com.zyozmen.products.adapter.out.mongodb.document;

import java.util.List;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagesDocument {

    @Field("foto_principal")
    private ImagenDocument fotoPrincipal;

    @Field("fotos_secundarias")
    private List<ImagenDocument> fotosSecundarias;
}
