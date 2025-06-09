package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @Column(name = "expiry_date", nullable = true)
    private LocalDateTime expiryDate;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Persons person;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}