package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Posts")
@Getter
@Setter
@NoArgsConstructor
public class Posts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PostID")
    private Long postId;

    @ManyToOne
    @JoinColumn(name = "CreatorID", nullable = false, foreignKey = @ForeignKey(name = "FK_Posts_Person"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Person creator;

    @Column(name = "Content", nullable = false, columnDefinition = "TEXT")
    private String content;
    @ManyToOne
    @JoinColumn(name = "RoomID", nullable = false, foreignKey = @ForeignKey(name = "FK_Posts_Room"))
    private Room room;
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Documents> documents;

    @Column(name = "CreatedAt", updatable = false, nullable = true)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comments> comments = new ArrayList<>();


    public Posts(Person creator, String content) {
        this.creator = creator;
        this.content = content;
    }

}
