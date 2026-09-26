package com.learning.miniblog.service;

import com.learning.miniblog.dto.PostRequest;
import com.learning.miniblog.dto.PostResponse;
import com.learning.miniblog.entity.Post;
import com.learning.miniblog.exception.ResourceNotFoundException;
import com.learning.miniblog.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

//pagination
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /* having all posts in a list
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    /* this is the old style writing.
    public List<PostResponse> getAllPosts(){
        List<Post> postListAll = postRepository.findAll();// Get all Post entities from database
        List<PostResponse> posts = new ArrayList<>(); // Create empty list for DTO responses

        for(Post post : postListAll){
            PostResponse response = toResponse(post);// Convert every Post entity to PostResponse DTO
            posts.add(response);
        }
        return posts;
    }*/
    //*/

    public Page<PostResponse> getAllPosts(int page, int size){
        Pageable pageable = PageRequest.of(page,size);

        return postRepository.findAll(pageable).map(post -> toResponse(post));///.map(this::toResponse)
        //can not use for loop on page as page though can be considered as kind of a list but posts will be hold on anotger block called content. doing this map() function can convert regular post to DTO one which is done by spring.
    }

    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        return toResponse(post);
    }

    public PostResponse createPost(PostRequest request) {
        Post post = toEntity(request);
        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    public PostResponse updatePost(Long id, PostRequest request) {
        Post existing = postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

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