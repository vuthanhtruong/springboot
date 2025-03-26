package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "Slots")
@Getter
@Setter
@NoArgsConstructor
public class Slots {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SlotID")
    private Long slotId;

    @Column(name = "SlotName", nullable = false, length = 50)
    private String slotName;

    @Column(name = "StartTime", nullable = true)
    private LocalTime startTime;

    @Column(name = "EndTime", nullable = true)
    private LocalTime endTime;

    // Constructor có tham số
    public Slots(String slotName, LocalTime startTime, LocalTime endTime) {
        this.slotName = slotName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}