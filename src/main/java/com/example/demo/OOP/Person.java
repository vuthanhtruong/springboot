package com.example.demo.OOP;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Person {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "FirstName", nullable = true, length = 100)
    private String firstName;

    @Column(name = "LastName", nullable = true, length = 100)
    private String lastName;

    @Column(name = "Email", nullable = false, unique = true, length = 255)
    @Email(message = "Email không hợp lệ")
    private String email;

    @Column(name = "PhoneNumber", nullable = false, unique = true, length = 20)
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @Column(name = "BirthDate", nullable = true)
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate birthDate; // Ngày sinh (yyyy-MM-dd)

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
