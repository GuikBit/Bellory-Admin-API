package org.exemplo.belloryadmin.service.lead;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Rate limiter sliding window via Redis sorted set, conforme spec §2.2.
 *
 * <p>Limites por IP (configuravel via properties):</p>
 * <ul>
 *   <li>3 envios / minuto</li>
 *   <li>10 envios / hora</li>
 *   <li>30 envios / dia</li>
 * </ul>
 *
 * <p>Implementacao: para cada janela mantemos um sorted set
 * {@code contato:{window}:{ip}} onde score = timestamp. A cada request:</p>
 * <ol>
 *   <li>Remover entradas mais antigas que a janela.</li>
 *   <li>Contar quantas restaram.</li>
 *   <li>Se &lt; limite, adicionar uma nova entrada e renovar TTL.</li>
 *   <li>Caso contrario, rejeitar.</li>
 * </ol>
 *
 * <p>Fail-open: se o Redis estiver indisponivel, deixamos passar
 * (logando warn). Preferimos perder protecao de spam a derrubar
 * o formulario para usuarios reais.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${bellory.contact.rate-limit.per-minute:3}")
    private int perMinute;

    @Value("${bellory.contact.rate-limit.per-hour:10}")
    private int perHour;

    @Value("${bellory.contact.rate-limit.per-day:30}")
    private int perDay;

    public RateLimitResult check(String ip) {
        if (ip == null || ip.isBlank()) ip = "unknown";

        try {
            if (!allowWindow("min",  ip, perMinute, Duration.ofMinutes(1))) {
                return new RateLimitResult(false, "minute");
            }
            if (!allowWindow("hour", ip, perHour, Duration.ofHours(1))) {
                return new RateLimitResult(false, "hour");
            }
            if (!allowWindow("day",  ip, perDay,  Duration.ofDays(1))) {
                return new RateLimitResult(false, "day");
            }
            return new RateLimitResult(true, null);
        } catch (Exception e) {
            log.warn("Falha no rate limiter Redis (fail-open) para IP {}: {}", ip, e.getMessage());
            return new RateLimitResult(true, null);
        }
    }

    private boolean allowWindow(String windowName, String ip, int limit, Duration window) {
        String key = "contato:" + windowName + ":" + ip;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - window.toMillis();

        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();

        // 1) Remove entradas fora da janela
        zset.removeRangeByScore(key, 0, windowStart);

        // 2) Conta atuais
        Long count = zset.zCard(key);
        if (count != null && count >= limit) {
            return false;
        }

        // 3) Adiciona nova entrada (membro unico para nao colidir em ms iguais)
        String member = now + ":" + Math.random();
        zset.add(key, member, now);

        // 4) Renova TTL um pouco maior que a janela (folga p/ sliding)
        redisTemplate.expire(key, window.plusMinutes(1));

        return true;
    }

    /**
     * Util para inspecionar quantas entradas existem em uma janela
     * (debug / observabilidade).
     */
    public long countInWindow(String ip, String windowName) {
        try {
            String key = "contato:" + windowName + ":" + ip;
            Set<String> all = redisTemplate.opsForZSet().range(key, 0, -1);
            return all == null ? 0 : all.size();
        } catch (Exception e) {
            return -1;
        }
    }

    public record RateLimitResult(boolean allowed, String windowExceeded) { }
}
