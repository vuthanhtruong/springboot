package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

@Entity
@Table(name = "Admin")
@PrimaryKeyJoinColumn(name = "ID") // Liên kết với khóa chính của Person
@Getter
@Setter
@OnDelete(action = OnDeleteAction.CASCADE)
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
        this.setPassword(password);
    }

    // Constructor không tham số (cần thiết cho JPA)
    public Admin() {
    }

    // Thêm setter để tự động mã hóa khi đặt mật khẩu mới
    public void setPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        this.password = passwordEncoder.encode(password);
    }

}