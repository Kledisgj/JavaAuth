package com.authbaseapp.authapp.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
// @Table(name = "users_app")
@Getter
@Setter
public class UsersApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String role;

    private boolean isDelete;

    public UsersApp() {
    }

    public UsersApp(Long id, String name, String email, String password, String role, boolean isDelete) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isDelete = isDelete;
    }
}
