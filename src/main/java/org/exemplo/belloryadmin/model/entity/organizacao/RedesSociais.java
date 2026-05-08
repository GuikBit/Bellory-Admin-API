package org.exemplo.belloryadmin.model.entity.organizacao;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedesSociais {
    private String instagram;
    private String facebook;
    private String whatsapp;
    private String linkedin;
    private String messenger;
    private String site;
    private String youtube;
}
