package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Table(name = "Dashboard")
@Getter
@Setter
public class Dashboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor", nullable = false)
    private LocalDate visitor;

    public Dashboard() {
        this.visitor = LocalDate.now();
    }
}