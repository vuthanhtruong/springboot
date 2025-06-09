package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;

@Entity
@Table(name = "Teachers")
@PrimaryKeyJoinColumn(name = "ID") // Liên kết với khóa chính của Person
@Getter
@Setter
@OnDelete(action = OnDeleteAction.CASCADE)
public class Teachers extends Persons {

    // Mã hóa mật khẩu bằng BCrypt
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Column(name = "Password", nullable = false, length = 255)
    private String password;
    @Column(nullable = false, updatable = false)
    private LocalDate createdDate = LocalDate.now();
    @Column(name = "MIS_ID", length = 100)
    private String misID;
    @ManyToOne
    @JoinColumn(name = "EmployeeID", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Staffs employee; // Liên kết với Employee (có thể là NULL)
    @ManyToOne
    @JoinColumn(name = "AdminID", nullable = true) // Có thể NULL nếu không có Admin trực tiếp quản lý
    private Admin admin;

    // Constructor có tham số
    public Teachers(String id, String password, String firstName, String lastName, String email, String phoneNumber, String misID, Staffs employee, Admin admin) {
        super(); // Gọi constructor của Person
        this.setId(id);
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setEmail(email);
        this.setPhoneNumber(phoneNumber);
        this.password = passwordEncoder.encode(password); // Mã hóa mật khẩu
        this.misID = misID;
        this.employee = employee;
        this.admin = admin;
    }

    // Constructor không tham số (cần thiết cho JPA)
    public Teachers() {
    }

    // Thêm setter để tự động mã hóa khi đặt mật khẩu mới
    public void setPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        this.password = passwordEncoder.encode(password);
    }
}
