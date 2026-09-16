package com.learning.miniblog.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private Long id;

    private String title;

    private String content;

    private String author;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}