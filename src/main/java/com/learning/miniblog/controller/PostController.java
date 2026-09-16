package com.learning.miniblog.controller;

import com.learning.miniblog.dto.PostRequest;
import com.learning.miniblog.dto.PostResponse;
import com.learning.miniblog.service.PostService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<PostResponse> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{id}")
    public PostResponse getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PostMapping
    public PostResponse createPost(@RequestBody PostRequest request) {
        return postService.createPost(request);
    }

    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id, @RequestBody PostRequest request) {
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id) {
        postService.deletePost(id);
    }
}