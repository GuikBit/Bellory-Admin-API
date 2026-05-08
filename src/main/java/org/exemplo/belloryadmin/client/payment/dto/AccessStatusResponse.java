package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessStatusResponse {
    private Long customerId;
    private String customerName;
    private Boolean allowed;
    private List<String> reasons;
    private String customBlockMessage;
    private AccessSummary summary;
    private LocalDateTime checkedAt;
}
