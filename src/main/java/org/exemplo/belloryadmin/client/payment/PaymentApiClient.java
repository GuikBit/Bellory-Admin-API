package org.exemplo.belloryadmin.client.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.dto.*;
import org.exemplo.belloryadmin.exception.PaymentApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PaymentApiClient {

    private final RestClient restClient;
    private final Long companyId;

    public PaymentApiClient(
            @Value("${payment.api.url}") String baseUrl,
            @Value("${payment.api.key}") String apiKey,
            @Value("${payment.api.company-id}") Long companyId,
            @Value("${payment.api.timeout-ms:3000}") int timeoutMs) {

        this.companyId = companyId;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(normalizedBase)
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("X-Company-Id", String.valueOf(companyId))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Long getCompanyId() {
        return companyId;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest req) {
        log.info("Payment API >> POST /api/v1/customers name='{}' document='{}' email='{}'",
                req.getName(), req.getDocument(), req.getEmail());
        log.debug("Payment API >> POST /api/v1/customers body={}", req);
        try {
            CustomerResponse resp = restClient.post()
                    .uri("/api/v1/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /customers", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CustomerResponse.class);
            log.info("Payment API << POST /api/v1/customers OK id={} name='{}'",
                    resp != null ? resp.getId() : null, resp != null ? resp.getName() : null);
            return resp;
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /customers", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public CustomerResponse getCustomer(Long customerId) {
        try {
            return restClient.get()
                    .uri("/api/v1/customers/{id}", customerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /customers/" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CustomerResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /customers/" + customerId, e);
        }
    }

    public void deleteCustomer(Long customerId) {
        try {
            restClient.delete()
                    .uri("/api/v1/customers/{id}", customerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("DELETE /customers/" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em DELETE /customers/" + customerId, e);
        }
    }

    public SubscriptionResponse createSubscription(CreateSubscriptionRequest req) {
        log.info("Payment API >> POST /api/v1/subscriptions customerId={} planId={} cycle={} externalRef='{}'",
                req.getCustomerId(), req.getPlanId(), req.getCycle(), req.getExternalReference());
        log.debug("Payment API >> POST /api/v1/subscriptions body={}", req);
        try {
            SubscriptionResponse resp = restClient.post()
                    .uri("/api/v1/subscriptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /subscriptions", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
            log.info("Payment API << POST /api/v1/subscriptions OK id={} status={}",
                    resp != null ? resp.getId() : null, resp != null ? resp.getStatus() : null);
            return resp;
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /subscriptions", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public SubscriptionResponse getSubscription(Long subscriptionId) {
        try {
            return restClient.get()
                    .uri("/api/v1/subscriptions/{id}", subscriptionId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions/" + subscriptionId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions/" + subscriptionId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<ChargeResponse> listChargesBySubscription(Long subscriptionId) {
        try {
            PageResponse<ChargeResponse> page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/subscriptions/{id}/charges")
                            .queryParam("size", 100)
                            .queryParam("sort", "createdAt,desc")
                            .build(subscriptionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions/" + subscriptionId + "/charges", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<ChargeResponse>>() {});
            if (page == null || page.getContent() == null) return Collections.emptyList();
            return page.getContent();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions/" + subscriptionId + "/charges", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<SubscriptionResponse> listSubscriptionsByCustomer(Long customerId) {
        try {
            PageResponse<SubscriptionResponse> page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/subscriptions")
                            .queryParam("customerId", customerId)
                            .queryParam("size", 50)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions?customerId=" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<SubscriptionResponse>>() {});
            if (page == null || page.getContent() == null) return Collections.emptyList();
            return page.getContent();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions customerId=" + customerId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<ChargeResponse> listChargesByCustomer(Long customerId) {
        try {
            PageResponse<ChargeResponse> page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/charges")
                            .queryParam("customerId", customerId)
                            .queryParam("size", 100)
                            .queryParam("sort", "createdAt,desc")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /charges?customerId=" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<ChargeResponse>>() {});
            if (page == null || page.getContent() == null) return Collections.emptyList();
            return page.getContent();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /charges customerId=" + customerId, e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResponse<ChargeResponse> listChargesByCustomerPaged(Long customerId, int page, int size) {
        try {
            PageResponse<ChargeResponse> resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/charges")
                            .queryParam("customerId", customerId)
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .queryParam("sort", "createdAt,desc")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /charges?customerId=" + customerId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<ChargeResponse>>() {});
            return resp != null ? resp : new PageResponse<>();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /charges customerId=" + customerId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public AccessStatusResponse getAccessStatus(Long customerId) {
        try {
            return restClient.get()
                    .uri("/api/v1/customers/{id}/access-status", customerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /customers/" + customerId + "/access-status", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(AccessStatusResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /customers/" + customerId + "/access-status", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public PlanResponse getPlan(Long planId) {
        try {
            return restClient.get()
                    .uri("/api/v1/plans/{id}", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public PlanResponse getPlanByCodigo(String codigo) {
        try {
            return restClient.get()
                    .uri("/api/v1/plans/codigo/{codigo}", codigo)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/codigo/" + codigo, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/codigo/" + codigo, e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<PlanLimitDto> getPlanLimits(Long planId) {
        try {
            List<PlanLimitDto> body = restClient.get()
                    .uri("/api/v1/plans/{id}/limits", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId + "/limits", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<List<PlanLimitDto>>() {});
            return body != null ? body : Collections.emptyList();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId + "/limits", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<PlanLimitDto> getPlanFeatures(Long planId) {
        try {
            List<PlanLimitDto> body = restClient.get()
                    .uri("/api/v1/plans/{id}/features", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId + "/features", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<List<PlanLimitDto>>() {});
            return body != null ? body : Collections.emptyList();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId + "/features", e);
        }
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public PlanLimitCheckResponse checkPlanLimit(Long planId, String key, Integer usage) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/api/v1/plans/{id}/limits/{key}");
                        if (usage != null) b.queryParam("usage", usage);
                        return b.build(planId, key);
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId + "/limits/" + key, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanLimitCheckResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId + "/limits/" + key, e);
        }
    }

    // ==================== SUBSCRIPTIONS (admin) ====================

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResponse<SubscriptionResponse> listSubscriptions(String status, Long customerId, int page, int size) {
        try {
            PageResponse<SubscriptionResponse> resp = restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/api/v1/subscriptions")
                                .queryParam("page", page)
                                .queryParam("size", size)
                                .queryParam("sort", "createdAt,desc");
                        if (status != null && !status.isBlank()) b.queryParam("status", status);
                        if (customerId != null) b.queryParam("customerId", customerId);
                        return b.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<SubscriptionResponse>>() {});
            return resp != null ? resp : new PageResponse<>();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions", e);
        }
    }

    public SubscriptionResponse updateSubscription(Long subscriptionId, UpdateSubscriptionRequest req) {
        log.info("Payment API >> PUT /subscriptions/{}", subscriptionId);
        try {
            return restClient.put()
                    .uri("/api/v1/subscriptions/{id}", subscriptionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("PUT /subscriptions/" + subscriptionId,
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em PUT /subscriptions/" + subscriptionId, e);
        }
    }

    public SubscriptionResponse pauseSubscription(Long subscriptionId, boolean confirmCouponRemoval) {
        log.info("Payment API >> POST /subscriptions/{}/pause", subscriptionId);
        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/subscriptions/{id}/pause")
                            .queryParam("confirmCouponRemoval", confirmCouponRemoval)
                            .build(subscriptionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /subscriptions/" + subscriptionId + "/pause",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /subscriptions/" + subscriptionId + "/pause", e);
        }
    }

    public SubscriptionResponse resumeSubscription(Long subscriptionId) {
        log.info("Payment API >> POST /subscriptions/{}/resume", subscriptionId);
        try {
            return restClient.post()
                    .uri("/api/v1/subscriptions/{id}/resume", subscriptionId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /subscriptions/" + subscriptionId + "/resume",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /subscriptions/" + subscriptionId + "/resume", e);
        }
    }

    public SubscriptionResponse updatePaymentMethod(Long subscriptionId, UpdatePaymentMethodRequest req) {
        log.info("Payment API >> PATCH /subscriptions/{}/payment-method", subscriptionId);
        try {
            return restClient.patch()
                    .uri("/api/v1/subscriptions/{id}/payment-method", subscriptionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("PATCH /subscriptions/" + subscriptionId + "/payment-method",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em PATCH /subscriptions/" + subscriptionId + "/payment-method", e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResponse<PlanChangeResponse> getPlanChangeHistory(Long subscriptionId, int page, int size) {
        try {
            PageResponse<PlanChangeResponse> resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/subscriptions/{id}/plan-changes")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .queryParam("sort", "requestedAt,desc")
                            .build(subscriptionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /subscriptions/" + subscriptionId + "/plan-changes",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<PlanChangeResponse>>() {});
            return resp != null ? resp : new PageResponse<>();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /subscriptions/" + subscriptionId + "/plan-changes", e);
        }
    }

    public void cancelPlanChange(Long subscriptionId, Long changeId) {
        log.info("Payment API >> DELETE /subscriptions/{}/plan-changes/{}", subscriptionId, changeId);
        try {
            restClient.delete()
                    .uri("/api/v1/subscriptions/{subId}/plan-changes/{changeId}", subscriptionId, changeId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("DELETE /subscriptions/" + subscriptionId + "/plan-changes/" + changeId,
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em DELETE /subscriptions/" + subscriptionId + "/plan-changes/" + changeId, e);
        }
    }

    // ==================== CUSTOMERS (admin) ====================

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResponse<CustomerResponse> listCustomers(String search, int page, int size) {
        try {
            PageResponse<CustomerResponse> resp = restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/api/v1/customers")
                                .queryParam("page", page)
                                .queryParam("size", size)
                                .queryParam("sort", "createdAt,desc");
                        if (search != null && !search.isBlank()) b.queryParam("search", search);
                        return b.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /customers", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<CustomerResponse>>() {});
            return resp != null ? resp : new PageResponse<>();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /customers", e);
        }
    }

    // ==================== CHARGES ====================

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public ChargeResponse getCharge(Long chargeId) {
        try {
            return restClient.get()
                    .uri("/api/v1/charges/{id}", chargeId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /charges/" + chargeId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(ChargeResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /charges/" + chargeId, e);
        }
    }

    // ==================== PLANS ====================

    @Retryable(
            retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public List<PlanResponse> listPlans() {
        try {
            PageResponse<PlanResponse> page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/plans")
                            .queryParam("size", 50)
                            .queryParam("sort", "tierOrder,asc")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<PlanResponse>>() {});
            if (page == null || page.getContent() == null) return Collections.emptyList();
            return page.getContent();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans", e);
        }
    }

    public PlanResponse createPlan(CreatePlanRequest req) {
        log.info("Payment API >> POST /plans codigo='{}'", req.getCodigo());
        try {
            return restClient.post()
                    .uri("/api/v1/plans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /plans", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /plans", e);
        }
    }

    public PlanResponse updatePlan(Long planId, UpdatePlanRequest req) {
        log.info("Payment API >> PUT /plans/{}", planId);
        try {
            return restClient.put()
                    .uri("/api/v1/plans/{id}", planId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("PUT /plans/" + planId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em PUT /plans/" + planId, e);
        }
    }

    public PlanResponse activatePlan(Long planId) {
        log.info("Payment API >> PATCH /plans/{}/activate", planId);
        try {
            return restClient.patch()
                    .uri("/api/v1/plans/{id}/activate", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("PATCH /plans/" + planId + "/activate",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em PATCH /plans/" + planId + "/activate", e);
        }
    }

    public PlanResponse deactivatePlan(Long planId) {
        log.info("Payment API >> PATCH /plans/{}/deactivate", planId);
        try {
            return restClient.patch()
                    .uri("/api/v1/plans/{id}/deactivate", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("PATCH /plans/" + planId + "/deactivate",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em PATCH /plans/" + planId + "/deactivate", e);
        }
    }

    public void deletePlan(Long planId) {
        log.info("Payment API >> DELETE /plans/{}", planId);
        try {
            restClient.delete()
                    .uri("/api/v1/plans/{id}", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("DELETE /plans/" + planId, response.getStatusCode(), bodyAsString(response));
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em DELETE /plans/" + planId, e);
        }
    }

    public PlanResponse createNewPlanVersion(Long planId, UpdatePlanRequest req) {
        log.info("Payment API >> POST /plans/{}/new-version", planId);
        try {
            return restClient.post()
                    .uri("/api/v1/plans/{id}/new-version", planId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /plans/" + planId + "/new-version",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /plans/" + planId + "/new-version", e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PlanPricingResponse getPlanPricing(Long planId) {
        try {
            return restClient.get()
                    .uri("/api/v1/plans/{id}/pricing", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /plans/" + planId + "/pricing",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanPricingResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /plans/" + planId + "/pricing", e);
        }
    }

    // ==================== PLAN CHANGE ====================

    public PlanChangePreviewResponse previewPlanChange(Long subscriptionId, Long newPlanId) {
        log.info("Payment API >> POST /subscriptions/{}/preview-change?newPlanId={}", subscriptionId, newPlanId);
        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/subscriptions/{id}/preview-change")
                            .queryParam("newPlanId", newPlanId)
                            .build(subscriptionId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /subscriptions/" + subscriptionId + "/preview-change",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanChangePreviewResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /subscriptions/" + subscriptionId + "/preview-change", e);
        }
    }

    public PlanChangeResponse changePlan(Long subscriptionId, RequestPlanChangeRequest req) {
        log.info("Payment API >> POST /subscriptions/{}/change-plan newPlanId={}", subscriptionId, req.getNewPlanId());
        try {
            return restClient.post()
                    .uri("/api/v1/subscriptions/{id}/change-plan", subscriptionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /subscriptions/" + subscriptionId + "/change-plan",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(PlanChangeResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /subscriptions/" + subscriptionId + "/change-plan", e);
        }
    }

    // ==================== CANCEL / PAUSE / RESUME ====================

    public SubscriptionResponse cancelSubscription(Long subscriptionId) {
        log.info("Payment API >> DELETE /subscriptions/{}", subscriptionId);
        try {
            return restClient.delete()
                    .uri("/api/v1/subscriptions/{id}", subscriptionId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("DELETE /subscriptions/" + subscriptionId,
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(SubscriptionResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em DELETE /subscriptions/" + subscriptionId, e);
        }
    }

    // ==================== COUPONS ====================

    public CouponValidationResponse validateCouponPublic(ValidateCouponRequest req) {
        log.info("Payment API >> POST /coupons/validate/public code='{}'", req.getCouponCode());
        try {
            return restClient.post()
                    .uri("/api/v1/coupons/validate/public")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /coupons/validate/public",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CouponValidationResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /coupons/validate/public", e);
        }
    }

    public CouponResponse createCoupon(CreateCouponRequest req) {
        log.info("Payment API >> POST /coupons code='{}'", req.getCode());
        try {
            return restClient.post()
                    .uri("/api/v1/coupons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("POST /coupons", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CouponResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em POST /coupons", e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResponse<CouponResponse> listCoupons(int page, int size) {
        try {
            PageResponse<CouponResponse> resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/coupons")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .queryParam("sort", "createdAt,desc")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /coupons", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<CouponResponse>>() {});
            return resp != null ? resp : new PageResponse<>();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /coupons", e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResponse<CouponResponse> listActiveCoupons(int page, int size) {
        try {
            PageResponse<CouponResponse> resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/coupons/active")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /coupons/active", response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<CouponResponse>>() {});
            return resp != null ? resp : new PageResponse<>();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /coupons/active", e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public CouponResponse getCoupon(Long couponId) {
        try {
            return restClient.get()
                    .uri("/api/v1/coupons/{id}", couponId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /coupons/" + couponId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CouponResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /coupons/" + couponId, e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public CouponResponse getCouponByCode(String code) {
        try {
            return restClient.get()
                    .uri("/api/v1/coupons/code/{code}", code)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /coupons/code/" + code, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CouponResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /coupons/code/" + code, e);
        }
    }

    public CouponResponse updateCoupon(Long couponId, UpdateCouponRequest req) {
        log.info("Payment API >> PUT /coupons/{}", couponId);
        try {
            return restClient.put()
                    .uri("/api/v1/coupons/{id}", couponId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("PUT /coupons/" + couponId, response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CouponResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em PUT /coupons/" + couponId, e);
        }
    }

    public void deleteCoupon(Long couponId) {
        log.info("Payment API >> DELETE /coupons/{}", couponId);
        try {
            restClient.delete()
                    .uri("/api/v1/coupons/{id}", couponId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("DELETE /coupons/" + couponId, response.getStatusCode(), bodyAsString(response));
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em DELETE /coupons/" + couponId, e);
        }
    }

    public CouponResponse activateCoupon(Long couponId) {
        log.info("Payment API >> PATCH /coupons/{}/activate", couponId);
        try {
            return restClient.patch()
                    .uri("/api/v1/coupons/{id}/activate", couponId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("PATCH /coupons/" + couponId + "/activate",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(CouponResponse.class);
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em PATCH /coupons/" + couponId + "/activate", e);
        }
    }

    @Retryable(retryFor = { ResourceAccessException.class, IOException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public PageResponse<CouponUsageResponse> getCouponUsages(Long couponId, int page, int size) {
        try {
            PageResponse<CouponUsageResponse> resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/coupons/{id}/usages")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build(couponId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw buildException("GET /coupons/" + couponId + "/usages",
                                response.getStatusCode(), bodyAsString(response));
                    })
                    .body(new ParameterizedTypeReference<PageResponse<CouponUsageResponse>>() {});
            return resp != null ? resp : new PageResponse<>();
        } catch (ResourceAccessException e) {
            throw new PaymentApiException("Timeout/IO em GET /coupons/" + couponId + "/usages", e);
        }
    }

    private PaymentApiException buildException(String opLabel, HttpStatusCode status, String body) {
        int code = status != null ? status.value() : 0;
        String msg = "Payment API erro em " + opLabel + " (HTTP " + code + ")";
        log.warn("{} - body: {}", msg, body);
        return new PaymentApiException(msg, code, body);
    }

    private static String bodyAsString(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Wrapper simples pra desserializar páginas da Payment API (Spring Data Page).
     * Só ficamos com content — o restante (pageable/totalElements) pode ser ignorado.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageResponse<T> {
        private List<T> content;
        private Long totalElements;
        private Integer totalPages;
        private Integer number;
        private Integer size;
    }
}
