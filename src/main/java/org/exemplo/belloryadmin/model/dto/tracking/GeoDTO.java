package org.exemplo.belloryadmin.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoDTO {

    private String country;
    private String state;
    private String city;
    private Double latitude;
    private Double longitude;
    private String source;
}
