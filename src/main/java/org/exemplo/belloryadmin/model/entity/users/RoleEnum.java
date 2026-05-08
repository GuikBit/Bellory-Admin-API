package org.exemplo.belloryadmin.model.entity.users;

public enum RoleEnum {
    CLIENTE("ROLE_CLIENTE"),
    FUNCIONARIO("ROLE_FUNCIONARIO"),
    ADMIN("ROLE_ADMIN"),
    SUPERADMIN("ROLE_SUPERADMIN"),
    FINANCEIRO("ROLE_FINANCEIRO"),
    GERENTE("ROLE_GERENTE"),
    RECEPCAO("ROLE_RECEPCAO"),
    PLATFORM_ADMIN("ROLE_PLATFORM_ADMIN");

    private final String descricao;

    RoleEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }
}
