package tn.weeding.agenceevenementielle.services;

import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.weeding.agenceevenementielle.dto.authentification.RoleRequestDto;
import tn.weeding.agenceevenementielle.dto.authentification.RoleResponseDto;
import tn.weeding.agenceevenementielle.entities.Role;
import tn.weeding.agenceevenementielle.exceptions.RoleNotFoundException;
import tn.weeding.agenceevenementielle.repository.RoleRepository;
import java.util.List;

@Service
@Transactional
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class RoleServiceImpl implements RoleServiceInterface{

    private RoleRepository roleRepository;
    private CodeGeneratorService codeGeneratorService;

    @Override
    public RoleResponseDto creationRole(RoleRequestDto roleRequestDto) {
        if (roleRepository.existsByNom(roleRequestDto.getNom())) {
            throw new IllegalArgumentException("Un rôle avec ce nom existe déjà");
        }
        Role role = new Role(
                roleRequestDto.getNom(),
                roleRequestDto.getDescription());

       Role saved = roleRepository.save(role);
        return mapToResponse(saved);
    }

    @Override
    public RoleResponseDto updateRole(Long id, RoleRequestDto roleRequestDto) {
        Role role = roleRepository.findById(id).orElseThrow(()->new RoleNotFoundException(id));
        //verifie que le nom est unique
        if (!role.getNom().equals(roleRequestDto.getNom())&& roleRepository.existsByNom(roleRequestDto.getNom())) {
            throw new IllegalArgumentException("un rôle avec ce nom existe déjà");
        }
        role.setNom(roleRequestDto.getNom());
        role.setDescription(roleRequestDto.getDescription());
        Role updated = roleRepository.save(role);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponseDto getRoleParId(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(()->new RoleNotFoundException(id));

        return mapToResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToResponse).toList();
               // .collect(Collectors.toList());
    }

    @Override
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(()->new RoleNotFoundException(id));
        roleRepository.delete(role);
    }


    private RoleResponseDto mapToResponse(Role role) {
        return new RoleResponseDto(
                role.getIdRole(),
                role.getNom(),
                role.getDescription(),
                role.getCreationDate(),
                role.getModificationDate(),
                role.getActive()
        );
    }
}
