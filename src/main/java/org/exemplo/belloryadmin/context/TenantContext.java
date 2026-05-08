package org.exemplo.belloryadmin.context;

/**
 * Context holder simplificado para o Bellory-Admin-API.
 *
 * <p>Diferente do TenantContext do Bellory-API, aqui NAO armazenamos organizacaoId
 * porque tokens admin (PLATFORM_ADMIN) nao tem organizacao associada. Tambem nao
 * carregamos ConfigSistema (e por organizacao do cliente, nao do admin).</p>
 */
public class TenantContext {

    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUsername = new ThreadLocal<>();
    private static final ThreadLocal<String> currentRole = new ThreadLocal<>();

    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }

    public static Long getCurrentUserId() {
        return currentUserId.get();
    }

    public static void setCurrentUsername(String username) {
        currentUsername.set(username);
    }

    public static String getCurrentUsername() {
        return currentUsername.get();
    }

    public static void setCurrentRole(String role) {
        currentRole.set(role);
    }

    public static String getCurrentRole() {
        return currentRole.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentUsername.remove();
        currentRole.remove();
    }

    public static void setContext(Long userId, String username, String role) {
        setCurrentUserId(userId);
        setCurrentUsername(username);
        setCurrentRole(role);
    }
}
