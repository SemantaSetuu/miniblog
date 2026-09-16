package com.learning.miniblog.service;

import com.learning.miniblog.dto.PostRequest;
import com.learning.miniblog.dto.PostResponse;
import com.learning.miniblog.entity.Post;
import com.learning.miniblog.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null) return null;
        return toResponse(post);
    }

    public PostResponse createPost(PostRequest request) {
        Post post = toEntity(request);
        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    public PostResponse updatePost(Long id, PostRequest request) {
        Post existing = postRepository.findById(id).orElse(null);
        if (existing == null) return null;

        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setAuthor(request.getAuthor());

        Post saved = postRepository.save(existing);
        return toResponse(saved);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    private Post toEntity(PostRequest request) {
        return Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(request.getAuthor())
                .build();
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getAuthor())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}