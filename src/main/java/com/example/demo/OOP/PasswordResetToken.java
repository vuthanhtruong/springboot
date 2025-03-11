package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PasswordResetToken")
@Getter
@Setter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "otp", nullable = true, length = 6)
    private String otp;

    @Column(name = "reset_token", nullable = true, length = 36)
    private String resetToken;

    @Column(name = "expiry_date", nullable = true)
    private LocalDateTime expiryDate;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = true)
    private Person person;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}