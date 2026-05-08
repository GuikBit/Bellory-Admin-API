package org.exemplo.belloryadmin.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceDTO {

    private String deviceType;
    private String os;
    private String browser;
    private String screenSize;
    private String viewportSize;
    private boolean touchEnabled;
    private String language;
}
