package org.exemplo.belloryadmin.model.entity.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseAPI<T> {
    private boolean success;
    private String message;
    private T dados;
    private Integer errorCode;
    private Map<String, String> errors;
}
