package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("select distinct user from User user join user.roles role where user.enabled = true and role in :roles order by user.username")
    List<User> findEnabledUsersWithAnyRole(@Param("roles") Collection<Role> roles);
}
