package com.example.demo.OOP;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;


import java.util.Set;

@Entity
@Table(name = "Admin")
@PrimaryKeyJoinColumn(name = "ID") // Liên kết với khóa chính của Person
@Getter
@Setter
public class Admin extends Person {

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Teachers> teachers; // Đảm bảo đúng tên class

    // Constructor có tham số
    public Admin(String id, String password, String firstName, String lastName, String email, String phoneNumber) {
        super();
        this.setId(id);
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setEmail(email);
        this.setPhoneNumber(phoneNumber);
        this.password = password;
    }


    // Constructor không tham số (cần thiết cho JPA)
    public Admin() {
    }
}