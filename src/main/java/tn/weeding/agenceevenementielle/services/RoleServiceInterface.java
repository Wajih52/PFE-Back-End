package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.RoleRequestDto;
import tn.weeding.agenceevenementielle.dto.RoleResponseDto;
import tn.weeding.agenceevenementielle.entities.Role;

import java.util.List;

public interface RoleServiceInterface {
    RoleResponseDto creationRole(RoleRequestDto roleRequestDto);
    RoleResponseDto updateRole(Long id ,RoleRequestDto roleRequestDto);
    RoleResponseDto getRoleParId(Long id);
    List<RoleResponseDto> getAllRoles();
    void deleteRole(Long id);

}
