package tn.weeding.agenceevenementielle.controller;


import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.weeding.agenceevenementielle.dto.RoleRequestDto;
import tn.weeding.agenceevenementielle.dto.RoleResponseDto;
import tn.weeding.agenceevenementielle.services.RoleServiceInterface;

import java.util.List;

@RestController
@RequestMapping("/roles")
@AllArgsConstructor(onConstructor = @__(@Autowired))
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private RoleServiceInterface roleService;

    @PostMapping("/ajoutRole")
    public ResponseEntity<RoleResponseDto> ajoutRole(@Valid @RequestBody RoleRequestDto roleRequestDto) {
        RoleResponseDto roleCreated = roleService.creationRole(roleRequestDto);
        return ResponseEntity.status(201).body(roleCreated);
    }

    @GetMapping("/afficheRoles")
    public ResponseEntity<List<RoleResponseDto>> AfficheRoles() {
        return ResponseEntity.status(200).body(roleService.getAllRoles());
    }
    @GetMapping("/afficheUnRole/{id}")
    public ResponseEntity<RoleResponseDto> AfficheUnRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleParId(id));
    }



    @PutMapping("/modifierRole/{id}")
    public ResponseEntity<RoleResponseDto> modifierRole(@PathVariable Long id,@Valid @RequestBody RoleRequestDto roleRequestDto) {
        return ResponseEntity.ok(roleService.updateRole(id, roleRequestDto));
    }

    @DeleteMapping("/supprimerRole/{id}")
    public ResponseEntity<?> supprimerRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }


}
