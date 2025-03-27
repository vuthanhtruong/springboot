package com.example.demo.ModelOOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "Comments")
@Getter
@Setter
@NoArgsConstructor
public class Comments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CommentID")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CommenterID", nullable = false, foreignKey = @ForeignKey(name = "FK_Comments_Person"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Person commenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PostID", nullable = false, foreignKey = @ForeignKey(name = "FK_Comments_Posts"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Posts post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EventID", nullable = false, foreignKey = @ForeignKey(name = "FK_Comments_Events"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Events event;

    @Column(name = "Text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "CreatedAt", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public Comments(Person commenter, Posts post, String text) {
        this.commenter = commenter;
        this.post = post;

        this.text = text;
    }
}
