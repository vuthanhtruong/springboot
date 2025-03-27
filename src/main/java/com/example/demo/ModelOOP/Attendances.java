package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "Attendance")
@Getter
@Setter
@NoArgsConstructor
public class Attendances {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AttendanceID")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TeacherID", nullable = false, foreignKey = @ForeignKey(name = "FK_Attendance_Teacher"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Teachers teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudentID", nullable = false, foreignKey = @ForeignKey(name = "FK_Attendance_Student"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Students student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SlotID", nullable = false, foreignKey = @ForeignKey(name = "FK_Attendance_Slot"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Slots slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TimetableID", nullable = false, foreignKey = @ForeignKey(name = "FK_Attendance_Timetable"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Timetable timetable;

    @Column(name = "Status", nullable = false)
    private String status; // "Present", "Absent", "Late"

    @Column(name = "Note", nullable = true, columnDefinition = "TEXT")
    private String note;

    @Column(name = "Datetime", nullable = true)
    private LocalDateTime datetime;

    // Constructor có tham số
    public Attendances(Teachers teacher, Students student, Slots slot, String status, String note, LocalDateTime datetime) {
        this.teacher = teacher;
        this.student = student;
        this.slot = slot;
        this.status = status;
        this.note = note;
        this.datetime = datetime;
    }
}