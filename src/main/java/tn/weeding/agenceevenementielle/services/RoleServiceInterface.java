package tn.weeding.agenceevenementielle.services;

import tn.weeding.agenceevenementielle.dto.authentification.RoleRequestDto;
import tn.weeding.agenceevenementielle.dto.authentification.RoleResponseDto;

import java.util.List;

public interface RoleServiceInterface {
    RoleResponseDto creationRole(RoleRequestDto roleRequestDto);
    RoleResponseDto updateRole(Long id ,RoleRequestDto roleRequestDto);
    RoleResponseDto getRoleParId(Long id);
    List<RoleResponseDto> getAllRoles();
    void deleteRole(Long id);

}
