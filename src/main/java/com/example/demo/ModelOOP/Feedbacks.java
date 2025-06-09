package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "Feedbacks")
public class Feedbacks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackID")
    private Long feedbackId;

    // Người đánh giá là Student (Khóa ngoại)
    @ManyToOne
    @JoinColumn(name = "ReviewerID", nullable = false, foreignKey = @ForeignKey(name = "FK_Feedbacks_Reviewer"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Students reviewer;

    // Giáo viên được chọn để đánh giá (Khóa ngoại)
    @ManyToOne
    @JoinColumn(name = "TeacherID", nullable = false, foreignKey = @ForeignKey(name = "FK_Feedbacks_Teacher"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Teachers teacher;

    // Người nhận đánh giá là Employee (Khóa ngoại)
    @ManyToOne
    @JoinColumn(name = "ReceiverID", nullable = false, foreignKey = @ForeignKey(name = "FK_Feedbacks_Receiver"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Staffs receiver;

    // Liên kết với bảng Events
    @ManyToOne
    @JoinColumn(name = "EventID", nullable = false, foreignKey = @ForeignKey(name = "FK_Feedbacks_Event"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Notifications event;

    // Liên kết với bảng Room
    @ManyToOne
    @JoinColumn(name = "RoomID", nullable = false, foreignKey = @ForeignKey(name = "FK_Feedbacks_Room"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Room room;

    @Column(name = "Text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Feedbacks() {
        this.createdAt = LocalDateTime.now();
    }

    public Feedbacks(Students reviewer, Teachers teacher, Staffs receiver, Notifications event, Room room, String text) {
        this.reviewer = reviewer;
        this.teacher = teacher;
        this.receiver = receiver;
        this.event = event;
        this.room = room;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Students getReviewer() {
        return reviewer;
    }

    public void setReviewer(Students reviewer) {
        this.reviewer = reviewer;
    }

    public Teachers getTeacher() {
        return teacher;
    }

    public void setTeacher(Teachers teacher) {
        this.teacher = teacher;
    }

    public Staffs getReceiver() {
        return receiver;
    }

    public void setReceiver(Staffs receiver) {
        this.receiver = receiver;
    }

    public Notifications getEvent() {
        return event;
    }

    public void setEvent(Notifications event) {
        this.event = event;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}