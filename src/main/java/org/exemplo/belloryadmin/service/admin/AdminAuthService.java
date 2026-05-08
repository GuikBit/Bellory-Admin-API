package org.exemplo.belloryadmin.service.admin;

import org.exemplo.belloryadmin.model.dto.admin.auth.AdminUserInfoDTO;
import org.exemplo.belloryadmin.model.dto.admin.auth.AdminUsuarioCreateDTO;
import org.exemplo.belloryadmin.model.dto.admin.auth.AdminUsuarioUpdateDTO;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.exemplo.belloryadmin.model.repository.users.UsuarioAdminRepository;
import org.exemplo.belloryadmin.service.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminAuthService {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AdminAuthService(UsuarioAdminRepository usuarioAdminRepository,
                            PasswordEncoder passwordEncoder,
                            TokenService tokenService) {
        this.usuarioAdminRepository = usuarioAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Autentica usuario admin e retorna token JWT
     */
    public String authenticate(String username, String password) {
        UsuarioAdmin admin = usuarioAdminRepository.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!admin.isAtivo()) {
            throw new RuntimeException("Conta desativada");
        }

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Credenciais inválidas");
        }

        return tokenService.generateToken(admin);
    }

    /**
     * Busca usuario admin pelo username
     */
    public UsuarioAdmin findByUsername(String username) {
        return usuarioAdminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário admin não encontrado"));
    }

    /**
     * Converte entidade para DTO de info
     */
    public AdminUserInfoDTO toUserInfoDTO(UsuarioAdmin admin) {
        return AdminUserInfoDTO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .nomeCompleto(admin.getNomeCompleto())
                .email(admin.getEmail())
                .role(admin.getRole())
                .ativo(admin.isAtivo())
                .dtCriacao(admin.getDtCriacao())
                .build();
    }

    // --- CRUD ---

    public List<AdminUserInfoDTO> listarTodos() {
        return usuarioAdminRepository.findAll().stream()
                .map(this::toUserInfoDTO)
                .collect(Collectors.toList());
    }

    public AdminUserInfoDTO buscarPorId(Long id) {
        UsuarioAdmin admin = usuarioAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário admin não encontrado"));
        return toUserInfoDTO(admin);
    }

    @Transactional
    public AdminUserInfoDTO criar(AdminUsuarioCreateDTO dto) {
        if (usuarioAdminRepository.existsByUsername(dto.getUsername().trim().toLowerCase())) {
            throw new RuntimeException("Username já existe");
        }
        if (usuarioAdminRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Email já existe");
        }

        UsuarioAdmin admin = new UsuarioAdmin();
        admin.setUsername(dto.getUsername().trim().toLowerCase());
        admin.setNomeCompleto(dto.getNomeCompleto());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setEmail(dto.getEmail().trim().toLowerCase());
        admin.setRole("ROLE_PLATFORM_ADMIN");
        admin.setAtivo(true);
        admin.setDtCriacao(LocalDateTime.now());

        return toUserInfoDTO(usuarioAdminRepository.save(admin));
    }

    @Transactional
    public AdminUserInfoDTO atualizar(Long id, AdminUsuarioUpdateDTO dto) {
        UsuarioAdmin admin = usuarioAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário admin não encontrado"));

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String newUsername = dto.getUsername().trim().toLowerCase();
            if (!newUsername.equals(admin.getUsername()) && usuarioAdminRepository.existsByUsername(newUsername)) {
                throw new RuntimeException("Username já existe");
            }
            admin.setUsername(newUsername);
        }

        if (dto.getNomeCompleto() != null && !dto.getNomeCompleto().isBlank()) {
            admin.setNomeCompleto(dto.getNomeCompleto());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String newEmail = dto.getEmail().trim().toLowerCase();
            if (!newEmail.equals(admin.getEmail()) && usuarioAdminRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email já existe");
            }
            admin.setEmail(newEmail);
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        admin.setDtAtualizacao(LocalDateTime.now());
        return toUserInfoDTO(usuarioAdminRepository.save(admin));
    }

    @Transactional
    public void desativar(Long id) {
        UsuarioAdmin admin = usuarioAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário admin não encontrado"));
        admin.setAtivo(false);
        admin.setDtAtualizacao(LocalDateTime.now());
        usuarioAdminRepository.save(admin);
    }
}
