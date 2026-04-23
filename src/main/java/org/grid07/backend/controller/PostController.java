package org.grid07.backend.controller;
import org.grid07.backend.dto.*;
import org.grid07.backend.entity.*;
import org.grid07.backend.dto.AddComment;
import org.grid07.backend.dto.CreatePost;
import org.grid07.backend.dto.LikePost;
import org.grid07.backend.entity.Comment;
import org.grid07.backend.entity.Post;
import org.grid07.backend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/posts")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody CreatePost req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(req));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long postId,
            @RequestBody AddComment req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.addComment(postId, req));}

    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, String>> likePost(
            @PathVariable Long postId,
            @RequestBody LikePost req) {
        postService.likePost(postId, req);
        return ResponseEntity.ok(Map.of(
                "status", "liked",
                "postId", postId.toString()));
    }
}
