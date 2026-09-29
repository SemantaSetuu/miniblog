package com.learning.miniblog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    // Primary Key of comments table
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Actual comment text
    private String message;

    /*
     * Many Comments
     *        ↓
     * One Post
     *
     * Example:
     * Comment 1
     * Comment 2
     * Comment 3
     * all belong to:
     * Post 1
     */
    @ManyToOne

    /*
     * Create a foreign key column:
     * post_id
     * inside comments table
     */
    @JoinColumn(name = "post_id")
    private Post post;
}