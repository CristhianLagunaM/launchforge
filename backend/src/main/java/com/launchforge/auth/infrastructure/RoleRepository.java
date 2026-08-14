package com.launchforge.auth.infrastructure;

import com.launchforge.persistence.model.identity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByNameIgnoreCase(String name);
}
