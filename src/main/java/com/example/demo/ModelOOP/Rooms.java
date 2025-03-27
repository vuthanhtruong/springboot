package com.example.demo.ModelOOP;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Rooms")
@PrimaryKeyJoinColumn(name = "RoomID") // Liên kết với ID từ Room
@Getter
@Setter
@NoArgsConstructor
public class Rooms extends Room {

    @Column(name = "Address", nullable = true, length = 500)
    private String address;

    public Rooms(String roomId, String roomName, String address, Boolean status, Employees employee, LocalDateTime startTime, LocalDateTime endTime) {
        super(roomId, roomName, status, employee, startTime, endTime);
        this.address = address;
    }
}
