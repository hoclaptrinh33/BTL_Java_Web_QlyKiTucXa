package com.ktx.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;

import com.ktx.dto.PermissionDto;
import com.ktx.dto.RoleDto;

public interface RoleService {

    List<RoleDto> listRoles();

    Optional<RoleDto> findByCode(String code);

    RoleDto getRoleByCode(String code);

    List<PermissionDto> getOperationPermissions();

    List<PermissionDto> getAllPermissions();

    RoleDto createRole(RoleDto dto, Authentication auth);

    RoleDto updateRole(String code, RoleDto dto, Authentication auth);

    void deleteRole(String code);

    List<String> getRolesForUser(Long userId);

    void setRolesForUser(Long userId, List<String> roleCodes, String scope, List<Long> buildingIds);

    String getScopeForUser(Long userId);

    List<Long> getAssignedBuildingIdsForUser(Long userId);
}
