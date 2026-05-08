package org.exemplo.belloryadmin.model.entity.endereco;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Coordenadas {
    private String latitude;
    private String longitude;
}
