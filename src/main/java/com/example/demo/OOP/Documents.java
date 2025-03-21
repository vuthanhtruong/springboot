package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "Documents")
@Getter
@Setter
@NoArgsConstructor
public class Documents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DocumentID")
    private Long documentId;

    @Column(name = "DocumentTitle", nullable = false)
    private String documentTitle;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "FileData", columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "FilePath")
    private String filePath;

    @ManyToOne
    @JoinColumn(name = "CreatorID", nullable = false, foreignKey = @ForeignKey(name = "FK_Documents_Person"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Person creator;

    @ManyToOne
    @JoinColumn(name = "PostID", nullable = false, foreignKey = @ForeignKey(name = "FK_Documents_Posts"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Posts post;

    @ManyToOne
    @JoinColumn(name = "EventID", nullable = false, foreignKey = @ForeignKey(name = "FK_Documents_Events"))
    @OnDelete(action = OnDeleteAction.CASCADE) // Nếu sự kiện bị xóa, tài liệu cũng bị xóa
    private Events event;

    // Constructor có tham số
    public Documents(String documentTitle, byte[] fileData, String filePath, Person creator, Posts post, Events event) {
        this.documentTitle = documentTitle;
        this.fileData = fileData;
        this.filePath = filePath;
        this.creator = creator;
        this.post = post;
        this.event = event;
    }
}
