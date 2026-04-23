package org.grid07.backend.dto;

import lombok.Data;

@Data
public class CreatePost {
    private Long authorId;
    private String authorType; // USER or BOT
    private String content;
}
