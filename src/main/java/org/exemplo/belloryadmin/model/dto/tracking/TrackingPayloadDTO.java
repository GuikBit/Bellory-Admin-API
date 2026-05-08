package org.exemplo.belloryadmin.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingPayloadDTO {

    @NotNull
    @Valid
    private VisitorDTO visitor;

    @Valid
    private DeviceDTO device;

    @Valid
    private SessionDTO session;

    @Valid
    private GeoDTO geo;

    private List<@Valid TrackingEventDTO> events;

    @Valid
    private PerformanceDTO performance;
}
