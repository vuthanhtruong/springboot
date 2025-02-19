package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "ScheduleNotifications")
@Getter
@Setter
public class ScheduleNotifications {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_notification_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = true)
    private Employees sender;  // Người gửi thông báo

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Person member;  // Người nhận (có thể là Student hoặc Teacher)

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;  // Phòng học (có thể là OnlineRoom hoặc Room)

    @Column(name = "message", nullable = true, columnDefinition = "TEXT")
    private String message;
}
