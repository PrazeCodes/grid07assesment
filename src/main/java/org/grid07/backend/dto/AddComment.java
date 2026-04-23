package org.grid07.backend.dto;


import lombok.Data;

@Data
public class AddComment {
    private Long authorId;
    private String authorType; // USER or BOT
    private String content;
    private int depthLevel;
    private Long humanAuthorId;
}
