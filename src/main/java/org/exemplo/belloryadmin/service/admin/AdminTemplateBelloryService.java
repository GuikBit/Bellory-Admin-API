package org.exemplo.belloryadmin.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.context.TenantContext;
import org.exemplo.belloryadmin.model.dto.template.*;
import org.exemplo.belloryadmin.model.entity.template.CategoriaTemplate;
import org.exemplo.belloryadmin.model.entity.template.TemplateBellory;
import org.exemplo.belloryadmin.model.entity.template.TipoTemplate;
import org.exemplo.belloryadmin.model.mapper.TemplateBelloryMapper;
import org.exemplo.belloryadmin.model.repository.template.TemplateBelloryRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTemplateBelloryService {

    private final TemplateBelloryRepository templateRepository;
    private final TemplateBelloryMapper mapper;
    private final ResourceLoader resourceLoader;

    // ===================== Listar =====================

    @Transactional(readOnly = true)
    public List<TemplateBelloryResponseDTO> listarTodos(TipoTemplate tipo, CategoriaTemplate categoria) {
        List<TemplateBellory> templates;

        if (tipo != null && categoria != null) {
            templates = templateRepository.findByTipoAndCategoriaAndAtivoTrueOrderByNomeAsc(tipo, categoria);
        } else if (tipo != null) {
            templates = templateRepository.findByTipoAndAtivoTrueOrderByCategoriaAsc(tipo);
        } else if (categoria != null) {
            templates = templateRepository.findByCategoriaAndAtivoTrueOrderByTipoAsc(categoria);
        } else {
            templates = templateRepository.findByAtivoTrueOrderByTipoAscCategoriaAsc();
        }

        return templates.stream()
                .map(this::toResponseComConteudoResolvido)
                .collect(Collectors.toList());
    }

    // ===================== Buscar por ID =====================

    @Transactional(readOnly = true)
    public TemplateBelloryResponseDTO buscarPorId(Long id) {
        TemplateBellory entity = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrado: " + id));
        return toResponseComConteudoResolvido(entity);
    }

    // ===================== Criar =====================

    @Transactional
    public TemplateBelloryResponseDTO criar(TemplateBelloryCreateDTO dto) {
        if (templateRepository.findByCodigo(dto.getCodigo()).isPresent()) {
            throw new RuntimeException("Ja existe um template com o codigo: " + dto.getCodigo());
        }

        TemplateBellory entity = mapper.toEntity(dto);
        entity.setUserCriacao(TenantContext.getCurrentUserId());

        TemplateBellory salvo = templateRepository.save(entity);
        log.info("Template criado: {} ({})", salvo.getNome(), salvo.getCodigo());

        return mapper.toResponseDTO(salvo);
    }

    // ===================== Atualizar =====================

    @Transactional
    public TemplateBelloryResponseDTO atualizar(Long id, TemplateBelloryUpdateDTO dto) {
        TemplateBellory entity = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrado: " + id));

        if (dto.getCodigo() != null && !dto.getCodigo().equals(entity.getCodigo())) {
            if (templateRepository.existsByCodigoAndIdNot(dto.getCodigo(), id)) {
                throw new RuntimeException("Ja existe um template com o codigo: " + dto.getCodigo());
            }
        }

        mapper.updateEntity(entity, dto);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());

        TemplateBellory salvo = templateRepository.save(entity);
        log.info("Template atualizado: {} ({})", salvo.getNome(), salvo.getCodigo());

        return mapper.toResponseDTO(salvo);
    }

    // ===================== Desativar (soft delete) =====================

    @Transactional
    public void desativar(Long id) {
        TemplateBellory entity = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrado: " + id));

        entity.setAtivo(false);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());
        templateRepository.save(entity);
        log.info("Template desativado: {} ({})", entity.getNome(), entity.getCodigo());
    }

    // ===================== Ativar =====================

    @Transactional
    public TemplateBelloryResponseDTO ativar(Long id) {
        TemplateBellory entity = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrado: " + id));

        entity.setAtivo(true);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());
        TemplateBellory salvo = templateRepository.save(entity);
        log.info("Template ativado: {} ({})", entity.getNome(), entity.getCodigo());

        return mapper.toResponseDTO(salvo);
    }

    // ===================== Marcar como padrao =====================

    @Transactional
    public TemplateBelloryResponseDTO marcarComoPadrao(Long id) {
        TemplateBellory entity = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrado: " + id));

        if (!entity.isAtivo()) {
            throw new RuntimeException("Não é possível marcar um template inativo como padrão");
        }

        // Desmarca o padrao anterior do mesmo tipo+categoria
        templateRepository.desmarcarPadrao(entity.getTipo(), entity.getCategoria(), entity.getId());

        entity.setPadrao(true);
        entity.setUserAtualizacao(TenantContext.getCurrentUserId());
        TemplateBellory salvo = templateRepository.save(entity);
        log.info("Template marcado como padrao: {} ({}) - tipo={}, categoria={}",
                entity.getNome(), entity.getCodigo(), entity.getTipo(), entity.getCategoria());

        return mapper.toResponseDTO(salvo);
    }

    // ===================== Preview =====================

    @Transactional(readOnly = true)
    public String preview(Long id, TemplatePreviewRequestDTO request) {
        TemplateBellory entity = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template não encontrado: " + id));

        String conteudo = resolverConteudoEmail(entity);

        Map<String, String> variaveis = request.getVariaveis();
        if (variaveis == null || variaveis.isEmpty()) {
            // Usa exemplos das variaveis disponiveis
            List<VariavelTemplateDTO> vars = mapper.parseVariaveis(entity.getVariaveisDisponiveis());
            for (VariavelTemplateDTO var : vars) {
                String placeholder = entity.getTipo() == TipoTemplate.WHATSAPP
                        ? "{{" + var.getNome() + "}}"
                        : "${" + var.getNome() + "}";
                conteudo = conteudo.replace(placeholder, var.getExemplo() != null ? var.getExemplo() : var.getNome());
            }
        } else {
            for (Map.Entry<String, String> entry : variaveis.entrySet()) {
                String placeholderWhatsApp = "{{" + entry.getKey() + "}}";
                String placeholderEmail = "${" + entry.getKey() + "}";
                conteudo = conteudo.replace(placeholderWhatsApp, entry.getValue());
                conteudo = conteudo.replace(placeholderEmail, entry.getValue());
            }
        }

        return conteudo;
    }

    // ===================== Helpers =====================

    /**
     * Converte entity para ResponseDTO resolvendo o conteudo de templates de email.
     * Se o conteudo for uma referencia a um arquivo HTML do classpath (sem tags HTML),
     * carrega o HTML real do arquivo para devolver ao frontend.
     */
    private TemplateBelloryResponseDTO toResponseComConteudoResolvido(TemplateBellory entity) {
        TemplateBelloryResponseDTO dto = mapper.toResponseDTO(entity);
        if (entity.getTipo() == TipoTemplate.EMAIL) {
            dto.setConteudo(resolverConteudoEmail(entity));
        }
        return dto;
    }

    /**
     * Se o conteudo de um template EMAIL for apenas um nome de referencia
     * (ex: "cobranca-aviso"), carrega o HTML real de templates/emails/{nome}.html.
     * Se ja for HTML completo ou se o arquivo nao existir, retorna o conteudo original.
     */
    private String resolverConteudoEmail(TemplateBellory entity) {
        String conteudo = entity.getConteudo();
        if (entity.getTipo() != TipoTemplate.EMAIL) {
            return conteudo;
        }
        // Se ja contem HTML, retorna direto
        if (conteudo != null && (conteudo.contains("<") || conteudo.contains("<!DOCTYPE"))) {
            return conteudo;
        }
        // Tenta carregar do classpath
        return carregarHtmlDoClasspath(conteudo);
    }

    /**
     * Carrega o conteudo HTML de um arquivo em templates/emails/{templateName}.html
     */
    private String carregarHtmlDoClasspath(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return templateName;
        }
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/emails/" + templateName + ".html");
            if (resource.exists()) {
                return resource.getContentAsString(StandardCharsets.UTF_8);
            }
            log.warn("Arquivo de template nao encontrado no classpath: templates/emails/{}.html", templateName);
            return templateName;
        } catch (IOException e) {
            log.error("Erro ao carregar template do classpath: {}", templateName, e);
            return templateName;
        }
    }
}
