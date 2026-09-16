package com.learning.miniblog.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    private String title;

    private String content;

    private String author;
}