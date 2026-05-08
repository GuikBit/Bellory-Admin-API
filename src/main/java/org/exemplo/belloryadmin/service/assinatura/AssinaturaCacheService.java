package org.exemplo.belloryadmin.service.assinatura;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.PaymentApiClient;
import org.exemplo.belloryadmin.client.payment.dto.AccessStatusResponse;
import org.exemplo.belloryadmin.client.payment.dto.CachedCustomerStatus;
import org.exemplo.belloryadmin.client.payment.dto.ChargeResponse;
import org.exemplo.belloryadmin.client.payment.dto.PaymentSubscriptionStatus;
import org.exemplo.belloryadmin.client.payment.dto.PlanLimitDto;
import org.exemplo.belloryadmin.client.payment.dto.PlanResponse;
import org.exemplo.belloryadmin.client.payment.dto.SubscriptionResponse;
import org.exemplo.belloryadmin.exception.PaymentApiException;
import org.exemplo.belloryadmin.model.dto.assinatura.AssinaturaStatusDTO;
import org.exemplo.belloryadmin.model.dto.assinatura.CobrancaPendenteDTO;
import org.exemplo.belloryadmin.model.entity.assinatura.Assinatura;
import org.exemplo.belloryadmin.model.repository.assinatura.AssinaturaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cache fresh (5min)+ stale (24h) do status consolidado de um customer na Payment API.
 * Fail-open: se a API cair e houver stale, serve o stale com log; se tudo falhar, propaga PaymentApiException.
 * Invalidação explícita via {@link #invalidate(Long)} — chamado pelo endpoint POST /assinatura/refresh-cache.
 */
@Slf4j
@Service
public class AssinaturaCacheService {

    private static final String KEY_PREFIX = "payment:status:";
    private static final String KEY_FRESH = ":fresh";
    private static final String KEY_STALE = ":stale";

    private final PaymentApiClient client;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final AssinaturaRepository assinaturaRepository;
    private final Duration freshTtl;
    private final Duration staleTtl;

    public AssinaturaCacheService(
            PaymentApiClient client,
            StringRedisTemplate paymentApiRedisTemplate,
            @Qualifier("paymentApiObjectMapper") ObjectMapper mapper,
            AssinaturaRepository assinaturaRepository,
            @Value("${payment.cache.fresh-ttl-seconds:300}") long freshSecs,
            @Value("${payment.cache.stale-ttl-seconds:86400}") long staleSecs) {
        this.client = client;
        this.redis = paymentApiRedisTemplate;
        this.mapper = mapper;
        this.assinaturaRepository = assinaturaRepository;
        this.freshTtl = Duration.ofSeconds(freshSecs);
        this.staleTtl = Duration.ofSeconds(staleSecs);
    }

    public CachedCustomerStatus get(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId obrigatorio");
        }

        String freshJson = tryRead(key(customerId, KEY_FRESH));
        if (freshJson != null) {
            CachedCustomerStatus cached = deserialize(freshJson);
            if (cached != null) return cached;
        }

        try {
            CachedCustomerStatus fromApi = fetchFromApi(customerId);
            writeCache(customerId, fromApi);
            return fromApi;
        } catch (PaymentApiException apiEx) {
            String staleJson = tryRead(key(customerId, KEY_STALE));
            if (staleJson != null) {
                CachedCustomerStatus stale = deserialize(staleJson);
                if (stale != null) {
                    log.warn("Payment API indisponivel, servindo STALE para customerId={} (fetchedAt={}): {}",
                            customerId, stale.getFetchedAt(), apiEx.getMessage());
                    return stale;
                }
            }
            log.error("Payment API indisponivel e sem stale para customerId={}: {}", customerId, apiEx.getMessage());
            throw apiEx;
        }
    }

    public CachedCustomerStatus refresh(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId obrigatorio");
        }
        CachedCustomerStatus fresh = fetchFromApi(customerId);
        writeCache(customerId, fresh);
        return fresh;
    }

    public void invalidate(Long customerId) {
        if (customerId == null) return;
        try {
            redis.delete(key(customerId, KEY_FRESH));
            redis.delete(key(customerId, KEY_STALE));
        } catch (Exception e) {
            log.warn("Erro invalidando cache customerId={}: {}", customerId, e.getMessage());
        }
    }

    private CachedCustomerStatus fetchFromApi(Long customerId) {
        AccessStatusResponse access = client.getAccessStatus(customerId);
        List<SubscriptionResponse> subs = client.listSubscriptionsByCustomer(customerId);
        SubscriptionResponse active = pickActiveOrLatest(subs);

        Long planId = active != null ? active.getPlanId() : null;
        PlanResponse plan = planId != null ? client.getPlan(planId) : null;
        List<PlanLimitDto> limits = planId != null ? client.getPlanLimits(planId) : Collections.emptyList();
        List<PlanLimitDto> features = planId != null ? client.getPlanFeatures(planId) : Collections.emptyList();

        List<ChargeResponse> cobrancasPendentes = fetchCobrancasPendentesSeNecessario(customerId, access);

        return CachedCustomerStatus.builder()
                .customerId(customerId)
                .customerName(access != null ? access.getCustomerName() : null)
                .allowed(access != null && Boolean.TRUE.equals(access.getAllowed()))
                .reasons(access != null ? access.getReasons() : Collections.emptyList())
                .customBlockMessage(access != null ? access.getCustomBlockMessage() : null)
                .summary(access != null ? access.getSummary() : null)
                .subscriptionId(active != null ? active.getId() : null)
                .subscriptionStatus(active != null ? active.getStatus() : null)
                .cycle(active != null ? active.getCycle() : null)
                .billingType(active != null ? active.getBillingType() : null)
                .nextDueDate(active != null ? active.getNextDueDate() : null)
                .effectivePrice(active != null ? active.getEffectivePrice() : null)
                .inTrial(active != null ? active.getInTrial() : null)
                .trialEndDate(active != null ? active.getTrialEndDate() : null)
                .planId(planId)
                .planCodigo(plan != null ? plan.getCodigo() : null)
                .planName(plan != null ? plan.getName() : null)
                .planIsFree(plan != null && Boolean.TRUE.equals(plan.getIsFree()))
                .limits(limits)
                .features(features)
                .cobrancasPendentes(cobrancasPendentes)
                .fetchedAt(LocalDateTime.now())
                .build();
    }

    private List<ChargeResponse> fetchCobrancasPendentesSeNecessario(Long customerId, AccessStatusResponse access) {
        if (access == null) return Collections.emptyList();
        boolean bloqueado = !Boolean.TRUE.equals(access.getAllowed());
        boolean temOverdueNoSummary = access.getSummary() != null
                && access.getSummary().getOverdueCharges() != null
                && access.getSummary().getOverdueCharges() > 0;
        if (!bloqueado && !temOverdueNoSummary) return Collections.emptyList();

        try {
            List<ChargeResponse> all = client.listChargesByCustomer(customerId);
            if (all == null || all.isEmpty()) return Collections.emptyList();
            LocalDate hoje = LocalDate.now();
            return all.stream()
                    .filter(c -> isCobrancaAguardandoPagamento(c, hoje))
                    .sorted(Comparator.comparing(ChargeResponse::getDueDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        } catch (PaymentApiException e) {
            log.warn("Falha ao buscar cobrancas pendentes customerId={}: {}", customerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isCobrancaAguardandoPagamento(ChargeResponse c, LocalDate hoje) {
        if (c == null || c.getStatus() == null) return false;
        String status = c.getStatus().toUpperCase();
        if ("OVERDUE".equals(status)) return true;
        if ("PENDING".equals(status)) {
            return c.getDueDate() != null && !c.getDueDate().isAfter(hoje);
        }
        return false;
    }

    private SubscriptionResponse pickActiveOrLatest(List<SubscriptionResponse> subs) {
        if (subs == null || subs.isEmpty()) return null;
        return subs.stream()
                .filter(s -> s.getStatus() == PaymentSubscriptionStatus.ACTIVE
                        || s.getStatus() == PaymentSubscriptionStatus.TRIALING)
                .findFirst()
                .orElse(subs.get(0));
    }

    private void writeCache(Long customerId, CachedCustomerStatus data) {
        try {
            String json = mapper.writeValueAsString(data);
            redis.opsForValue().set(key(customerId, KEY_FRESH), json, freshTtl);
            redis.opsForValue().set(key(customerId, KEY_STALE), json, staleTtl);
        } catch (Exception e) {
            log.warn("Erro gravando cache customerId={}: {}", customerId, e.getMessage());
        }
    }

    private String tryRead(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis indisponivel lendo '{}': {}", key, e.getMessage());
            return null;
        }
    }

    private CachedCustomerStatus deserialize(String json) {
        try {
            return mapper.readValue(json, CachedCustomerStatus.class);
        } catch (Exception e) {
            log.warn("Erro desserializando cache: {}", e.getMessage());
            return null;
        }
    }

    private static String key(Long customerId, String suffix) {
        return KEY_PREFIX + customerId + suffix;
    }

    // ==================== API de alto nivel por organizacao ====================

    /**
     * Retorna {@link AssinaturaStatusDTO} consolidado de uma organizacao.
     * Resolve o {@code paymentApiCustomerId} na Assinatura local e consulta o cache.
     * Fail-soft: se a assinatura ainda nao foi migrada ou se a Payment API estiver fora
     * e sem stale, retorna status "INDISPONIVEL" sem bloquear o login.
     */
    @Transactional(readOnly = true)
    public AssinaturaStatusDTO getStatusByOrganizacao(Long organizacaoId) {
        if (organizacaoId == null) {
            return semAssinatura();
        }
        Optional<Assinatura> opt = assinaturaRepository.findByOrganizacaoId(organizacaoId);
        if (opt.isEmpty()) return semAssinatura();
        Assinatura a = opt.get();
        Long customerId = a.getPaymentApiCustomerId();
        if (customerId == null) {
            log.warn("Assinatura org={} ainda sem paymentApiCustomerId — pendente de migracao", organizacaoId);
            return AssinaturaStatusDTO.builder()
                    .bloqueado(false)
                    .statusAssinatura("NAO_MIGRADO")
                    .situacao("ATIVA")
                    .mensagem("Assinatura ainda nao migrada para a Payment API")
                    .build();
        }
        try {
            return toStatusDTO(get(customerId));
        } catch (PaymentApiException e) {
            log.warn("Payment API indisponivel para org={}, customerId={}: {}", organizacaoId, customerId, e.getMessage());
            return AssinaturaStatusDTO.builder()
                    .bloqueado(false)
                    .statusAssinatura("INDISPONIVEL")
                    .situacao("ATIVA")
                    .mensagem("Nao foi possivel verificar o status da assinatura. Tente novamente em instantes.")
                    .build();
        }
    }

    /**
     * Retorna o snapshot cacheado completo (com limits/features) de uma organizacao.
     * Usado pelo {@code LimiteValidatorService} para decisoes granulares.
     * Retorna {@code null} se a assinatura ainda nao foi migrada ou se a Payment API
     * esta indisponivel sem stale.
     */
    @Transactional(readOnly = true)
    public CachedCustomerStatus getCachedByOrganizacao(Long organizacaoId) {
        if (organizacaoId == null) return null;
        return assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .map(Assinatura::getPaymentApiCustomerId)
                .map(customerId -> {
                    try {
                        return get(customerId);
                    } catch (PaymentApiException e) {
                        log.warn("Payment API indisponivel para org={}, customerId={}: {}",
                                organizacaoId, customerId, e.getMessage());
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * Forca refetch na Payment API + atualiza cache, a partir da organizacao.
     * Chamado pelo endpoint POST /api/v1/assinatura/refresh-cache.
     */
    @Transactional(readOnly = true)
    public AssinaturaStatusDTO refreshByOrganizacao(Long organizacaoId) {
        if (organizacaoId == null) return semAssinatura();
        Assinatura a = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElse(null);
        if (a == null || a.getPaymentApiCustomerId() == null) {
            return getStatusByOrganizacao(organizacaoId);
        }
        Long customerId = a.getPaymentApiCustomerId();
        invalidate(customerId);
        return toStatusDTO(refresh(customerId));
    }

    private AssinaturaStatusDTO semAssinatura() {
        return AssinaturaStatusDTO.builder()
                .bloqueado(false)
                .statusAssinatura("SEM_ASSINATURA")
                .situacao("SEM_ASSINATURA")
                .mensagem("Nenhuma assinatura encontrada")
                .build();
    }

    private AssinaturaStatusDTO toStatusDTO(CachedCustomerStatus s) {
        if (s == null) return semAssinatura();
        boolean bloqueado = !s.isAllowed();

        List<CobrancaPendenteDTO> cobrancas = s.getCobrancasPendentes() != null
                ? s.getCobrancasPendentes().stream().map(this::toCobrancaPendenteDTO).toList()
                : Collections.emptyList();

        LocalDate dtFimTrial = s.getTrialEndDate() != null ? s.getTrialEndDate().toLocalDate() : null;
        Integer diasRestantesTrial = null;
        if (Boolean.TRUE.equals(s.getInTrial()) && dtFimTrial != null) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dtFimTrial);
            diasRestantesTrial = (int) Math.max(0, dias);
        }

        return AssinaturaStatusDTO.builder()
                .bloqueado(bloqueado)
                .statusAssinatura(s.getSubscriptionStatus() != null ? s.getSubscriptionStatus().name() : "UNKNOWN")
                .situacao(determinarSituacao(s))
                .mensagem(mensagemDe(s))
                .planoCodigo(s.getPlanCodigo())
                .planoNome(s.getPlanName())
                .planoGratuito(s.isPlanIsFree())
                .planoLimites(s.getLimits())
                .planoFeatures(s.getFeatures())
                .diasRestantesTrial(diasRestantesTrial)
                .dtFimTrial(dtFimTrial)
                .cicloCobranca(s.getCycle() != null ? s.getCycle().name() : null)
                .dtProximoVencimento(s.getNextDueDate())
                .temCobrancaPendente(!cobrancas.isEmpty())
                .valorPendente(s.getSummary() != null ? s.getSummary().getTotalOverdueValue() : null)
                .cobrancasPendentes(cobrancas.isEmpty() ? null : cobrancas)
                .build();
    }

    private CobrancaPendenteDTO toCobrancaPendenteDTO(ChargeResponse c) {
        return CobrancaPendenteDTO.builder()
                .id(c.getId())
                .subscriptionId(c.getSubscriptionId())
                .valor(c.getValue())
                .valorOriginal(c.getOriginalValue())
                .dtVencimento(c.getDueDate())
                .status(c.getStatus())
                .billingType(c.getBillingType())
                .installmentNumber(c.getInstallmentNumber())
                .pixQrcode(c.getPixQrcode())
                .pixCopyPaste(c.getPixCopyPaste())
                .boletoUrl(c.getBoletoUrl())
                .invoiceUrl(c.getInvoiceUrl())
                .build();
    }

    private String determinarSituacao(CachedCustomerStatus s) {
        if (s.getSubscriptionStatus() == null) return "SEM_ASSINATURA";
        // Trial tem precedencia: a Payment API sinaliza inTrial=true mesmo se status virar ACTIVE
        // durante a janela de trial. Usar o flag dedicado (mais robusto que o status).
        if (Boolean.TRUE.equals(s.getInTrial())) return "TRIAL_ATIVO";
        if (s.isPlanIsFree()) return "PLANO_GRATUITO";
        if (!s.isAllowed()) {
            if (s.getSummary() != null && s.getSummary().getOverdueCharges() != null && s.getSummary().getOverdueCharges() > 0) {
                return "PAGAMENTO_ATRASADO";
            }
            return "SUSPENSA";
        }
        return switch (s.getSubscriptionStatus()) {
            case ACTIVE -> "ATIVA";
            case TRIALING -> "TRIAL_ATIVO";
            case PAUSED, SUSPENDED -> "SUSPENSA";
            case CANCELED -> "CANCELADA_SEM_ACESSO";
            case EXPIRED -> "VENCIDA";
        };
    }

    private String mensagemDe(CachedCustomerStatus s) {
        if (s.isAllowed()) return null;
        if (s.getCustomBlockMessage() != null && !s.getCustomBlockMessage().isBlank()) return s.getCustomBlockMessage();
        if (s.getReasons() != null && !s.getReasons().isEmpty()) return String.join("; ", s.getReasons());
        return "Acesso bloqueado";
    }
}
