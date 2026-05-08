package org.exemplo.belloryadmin.model.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.template.*;
import org.exemplo.belloryadmin.model.entity.template.TemplateBellory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateBelloryMapper {

    private final ObjectMapper objectMapper;

    // ===================== Entity -> Response DTO =====================

    public TemplateBelloryResponseDTO toResponseDTO(TemplateBellory entity) {
        if (entity == null) return null;

        return TemplateBelloryResponseDTO.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .nome(entity.getNome())
                .descricao(entity.getDescricao())
                .tipo(entity.getTipo())
                .categoria(entity.getCategoria())
                .assunto(entity.getAssunto())
                .conteudo(entity.getConteudo())
                .variaveisDisponiveis(parseVariaveis(entity.getVariaveisDisponiveis()))
                .ativo(entity.isAtivo())
                .padrao(entity.isPadrao())
                .icone(entity.getIcone())
                .dtCriacao(entity.getDtCriacao())
                .dtAtualizacao(entity.getDtAtualizacao())
                .userCriacao(entity.getUserCriacao())
                .userAtualizacao(entity.getUserAtualizacao())
                .build();
    }

    // ===================== Create DTO -> Entity =====================

    public TemplateBellory toEntity(TemplateBelloryCreateDTO dto) {
        if (dto == null) return null;

        TemplateBellory entity = new TemplateBellory();
        entity.setCodigo(dto.getCodigo());
        entity.setNome(dto.getNome());
        entity.setDescricao(dto.getDescricao());
        entity.setTipo(dto.getTipo());
        entity.setCategoria(dto.getCategoria());
        entity.setAssunto(dto.getAssunto());
        entity.setConteudo(dto.getConteudo());
        entity.setVariaveisDisponiveis(serializeVariaveis(dto.getVariaveisDisponiveis()));
        entity.setIcone(dto.getIcone());
        entity.setAtivo(true);
        return entity;
    }

    // ===================== Update DTO -> Entity (partial) =====================

    public void updateEntity(TemplateBellory entity, TemplateBelloryUpdateDTO dto) {
        if (dto == null || entity == null) return;

        if (dto.getCodigo() != null) entity.setCodigo(dto.getCodigo());
        if (dto.getNome() != null) entity.setNome(dto.getNome());
        if (dto.getDescricao() != null) entity.setDescricao(dto.getDescricao());
        if (dto.getTipo() != null) entity.setTipo(dto.getTipo());
        if (dto.getCategoria() != null) entity.setCategoria(dto.getCategoria());
        if (dto.getAssunto() != null) entity.setAssunto(dto.getAssunto());
        if (dto.getConteudo() != null) entity.setConteudo(dto.getConteudo());
        if (dto.getVariaveisDisponiveis() != null) entity.setVariaveisDisponiveis(serializeVariaveis(dto.getVariaveisDisponiveis()));
        if (dto.getIcone() != null) entity.setIcone(dto.getIcone());
    }

    // ===================== JSONB helpers =====================

    public List<VariavelTemplateDTO> parseVariaveis(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<VariavelTemplateDTO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Erro ao fazer parse das variaveis do template: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public String serializeVariaveis(List<VariavelTemplateDTO> variaveis) {
        if (variaveis == null || variaveis.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(variaveis);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar variaveis do template: {}", e.getMessage());
            return null;
        }
    }
}
