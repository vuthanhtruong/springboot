package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "ClassroomDetails")
@Getter
@Setter
@NoArgsConstructor
public class ClassroomDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ClassroomDetailsID")
    private Long classroomDetailsId;

    @ManyToOne
    @JoinColumn(name = "RoomID", nullable = true, foreignKey = @ForeignKey(name = "FK_ClassroomDetails_Room"))
    @OnDelete(action = OnDeleteAction.CASCADE) // Nếu phòng học bị xóa, chi tiết lớp học cũng bị xóa
    private Room room;

    @ManyToOne
    @JoinColumn(name = "MemberID", nullable = true, foreignKey = @ForeignKey(name = "FK_ClassroomDetails_Person"))
    @OnDelete(action = OnDeleteAction.CASCADE) // Nếu thành viên bị xóa, dữ liệu liên quan cũng bị xóa
    private Person member;

    @ManyToOne
    @JoinColumn(name = "EventID", nullable = false, foreignKey = @ForeignKey(name = "FK_ClassroomDetails_Event"))
    @OnDelete(action = OnDeleteAction.CASCADE) // Nếu sự kiện bị xóa, dữ liệu chi tiết lớp học cũng bị xóa
    private Events event;

    // Constructor có tham số
    public ClassroomDetails(Room room, Person member) {
        this.room = room;
        this.member = member;
    }
}
