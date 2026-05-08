package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerResponse {
    private Long id;
    private Long companyId;
    private String asaasId;
    private String name;
    private String document;
    private String email;
    private String phone;
    private String addressStreet;
    private String addressNumber;
    private String addressComplement;
    private String addressNeighborhood;
    private String addressCity;
    private String addressState;
    private String addressPostalCode;
    private BigDecimal creditBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
