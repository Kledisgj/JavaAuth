package com.authbaseapp.authapp.Repository;

import com.authbaseapp.authapp.Entity.UsersApp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsersAppRepository extends JpaRepository<UsersApp, Long> {

    Optional<UsersApp> findByEmail(String email);

    boolean existsByEmail(String email);

}
