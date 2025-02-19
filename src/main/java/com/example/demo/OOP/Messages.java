package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "Messages")
@Getter
@Setter
public class Messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MessagesID")
    private Integer messagesID;

    @ManyToOne
    @JoinColumn(name = "MessageSenderID", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // Thêm ON DELETE CASCADE
    private Person sender;  // Người gửi tin nhắn

    @ManyToOne
    @JoinColumn(name = "MessageRecipientID", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // Thêm ON DELETE CASCADE
    private Person recipient;  // Người nhận tin nhắn

    @Column(name = "Datetime", nullable = true)
    private LocalDateTime datetime;

    @Column(name = "Text", nullable = true, columnDefinition = "TEXT")
    private String text;

    // Constructors
    public Messages() {
    }

    public Messages(Person sender, Person recipient, LocalDateTime datetime, String text) {
        this.sender = sender;
        this.recipient = recipient;
        this.datetime = datetime;
        this.text = text;
    }
}