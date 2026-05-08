package org.exemplo.belloryadmin.model.dto.lead.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Coluna do kanban: status + leads daquele status.
 * Lista completa eh retornada por GET /api/v1/admin/leads/kanban
 * — cada elemento eh uma coluna na ordem definida.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadKanbanColumnDTO {
    private LeadStatusDTO status;
    private List<LeadListDTO> leads;
    private long total;
}
