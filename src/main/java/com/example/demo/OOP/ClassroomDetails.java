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
    @OnDelete(action = OnDeleteAction.CASCADE) // Thêm ON DELETE CASCADE
    private Room room;

    @ManyToOne
    @JoinColumn(name = "MemberID", nullable = true, foreignKey = @ForeignKey(name = "FK_ClassroomDetails_Person"))
    @OnDelete(action = OnDeleteAction.CASCADE) // Thêm ON DELETE CASCADE
    private Person member;

    // Constructor có tham số
    public ClassroomDetails(Room room, Person member) {
        this.room = room;
        this.member = member;
    }
}
