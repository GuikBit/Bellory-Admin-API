package org.exemplo.belloryadmin.model.dto.template;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplatePreviewRequestDTO {

    private Map<String, String> variaveis;
}
