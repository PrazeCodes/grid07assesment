package org.grid07.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "post_id", nullable = false)
    private Long postId;
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    @Column(name = "author_type", nullable = false, length = 10)
    private String authorType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "depth_level", nullable = false)
    private int depthLevel = 0;@Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
