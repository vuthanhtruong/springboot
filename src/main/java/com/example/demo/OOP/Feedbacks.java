package com.example.demo.OOP;

import jakarta.persistence.*;
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
    @JoinColumn(name = "ReviewerID", nullable = false)
    private Students reviewer;

    // Giáo viên được chọn để đánh giá (Khóa ngoại)
    @ManyToOne
    @JoinColumn(name = "TeacherID", nullable = false)
    private Teachers teacher;

    // Người nhận đánh giá là Employee (Khóa ngoại)
    @ManyToOne
    @JoinColumn(name = "ReceiverID", nullable = false)
    private Employees receiver;

    @Column(name = "Text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Feedbacks() {
        this.createdAt = LocalDateTime.now();
    }

    public Feedbacks(Students reviewer, Teachers teacher, Employees receiver, String text) {
        this.reviewer = reviewer;
        this.teacher = teacher;
        this.receiver = receiver;
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

    public Employees getReceiver() {
        return receiver;
    }

    public void setReceiver(Employees receiver) {
        this.receiver = receiver;
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

