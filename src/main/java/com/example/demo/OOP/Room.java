package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED) // Chiến lược tạo bảng riêng cho từng lớp con
@Table(name = "Room") // Bảng chung cho các loại phòng
@Getter
@Setter
@NoArgsConstructor
public abstract class Room {

    @Id
    @Column(name = "RoomID")
    private String roomId;

    @Column(name = "RoomName", nullable = true, length = 255)
    private String roomName;

    @Column(name = "Status", nullable = true)
    private Boolean status; // true: hoạt động, false: không hoạt động

    @ManyToOne
    @JoinColumn(name = "EmployeeID", nullable = true, foreignKey = @ForeignKey(name = "FK_Room_Employee"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employees employee;

    @Column(name = "StartTime", nullable = true)
    private LocalDateTime startTime;

    @Column(name = "EndTime", nullable = true)
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Posts> posts = new ArrayList<>();


    // Constructor
    public Room(String roomId, String roomName, Boolean status, Employees employee, LocalDateTime startTime, LocalDateTime endTime) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.status = status;
        this.employee = employee;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
