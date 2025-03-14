package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Entity
@Table(name = "Employees")
@PrimaryKeyJoinColumn(name = "ID") // Liên kết với khóa chính từ Person
@Getter
@Setter
@OnDelete(action = OnDeleteAction.CASCADE)
public class Employees extends Person {

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @ManyToOne
    @JoinColumn(name = "AdminID", nullable = true) // Nhân viên phải thuộc một admin
    @OnDelete(action = OnDeleteAction.CASCADE) // Thêm ON DELETE CASCADE
    private Admin admin;

    // Constructor có tham số
    public Employees(String id, String firstName, String lastName, String email, String phoneNumber, String password, Admin admin) {
        super(); // Gọi constructor của Person
        this.setId(id);
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setEmail(email);
        this.setPhoneNumber(phoneNumber);
        this.setPassword(password); // Gán mật khẩu với mã hóa
        this.admin = admin;
    }

    // Constructor không tham số (cần thiết cho JPA)
    public Employees() {
    }

    // Ghi đè setter để mã hóa mật khẩu trước khi lưu
    public void setPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        this.password = passwordEncoder.encode(password);
    }
}
