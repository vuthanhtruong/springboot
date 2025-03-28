package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "Timetable")
@Getter
@Setter
@NoArgsConstructor
public class Timetable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TimetableID")
    private Long timetableId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoomID", nullable = true, foreignKey = @ForeignKey(name = "FK_Timetable_Room"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SlotID", nullable = true, foreignKey = @ForeignKey(name = "FK_Timetable_Slot"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Slots slot;

    @Column(name = "DayOfTheWeek", nullable = true)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Column(name = "Date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Editor", nullable = true, foreignKey = @ForeignKey(name = "FK_Timetable_Editor"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employees editor;

    // Constructor
    public Timetable(Room room, Slots slot, DayOfWeek dayOfWeek, Employees editor, LocalDate date) {
        this.room = room;
        this.slot = slot;
        this.dayOfWeek = dayOfWeek;
        this.editor = editor;
        this.date = date;
    }
}