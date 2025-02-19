package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "OnlineRooms")
@PrimaryKeyJoinColumn(name = "RoomID") // Liên kết với ID từ Room
@Getter
@Setter
@NoArgsConstructor
public class OnlineRooms extends Room {

    @Column(name = "Link", nullable = true, length = 500)
    private String link;

    public OnlineRooms(String roomId, String roomName, String link, Boolean status, Employees employee, LocalDateTime startTime, LocalDateTime endTime) {
        super(roomId, roomName, status, employee, startTime, endTime);
        this.link = link;
    }
}
