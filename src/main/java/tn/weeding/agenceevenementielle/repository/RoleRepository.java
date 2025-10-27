package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNom(String name);
    boolean existsByNom(String name);
}
