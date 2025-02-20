package com.example.demo.OOP;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Person creator;

    @ManyToOne
    @JoinColumn(name = "PostID", nullable = false, foreignKey = @ForeignKey(name = "FK_Documents_Posts"))
    private Posts post;
}
