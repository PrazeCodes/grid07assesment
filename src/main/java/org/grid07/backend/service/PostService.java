package org.grid07.backend.service;

import org.grid07.backend.dto.AddComment;
import org.grid07.backend.dto.CreatePost;
import org.grid07.backend.dto.LikePost;
import org.grid07.backend.entity.Comment;
import org.grid07.backend.entity.Post;
import org.grid07.backend.repository.CommentRepository;
import org.grid07.backend.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ViralityService viralityService;
    @Autowired private NotificationService notificationService;
    // create-post
    public Post createPost(CreatePost req) {
        Post p = new Post();
        p.setAuthorId(req.getAuthorId());
        p.setAuthorType(req.getAuthorType().toUpperCase());
        p.setContent(req.getContent());
        return postRepository.save(p);
    }
    // add-comment
    public Comment addComment(Long postId, AddComment req) {
        String authorType = req.getAuthorType().toUpperCase();
// STEP 1: Vertical cap
        if (req.getDepthLevel() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Comment depth exceeds max of 20");
        }
        if ("BOT".equals(authorType)) {
            if (req.getHumanAuthorId() != null &&
                    viralityService.isCooldownActive(
                            req.getAuthorId(), req.getHumanAuthorId())) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Bot on cooldown for this human (10-min limit)");
            }
            if (!viralityService.checkAndIncrementBotCount(postId)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Post hit max of 100 bot replies");
            }
            //set cooldown
            if (req.getHumanAuthorId() != null) {
                viralityService.setCooldown(
                        req.getAuthorId(), req.getHumanAuthorId());
            }
            viralityService.updateViralityScore(postId, "BOT_REPLY");
            notificationService.handleBotInteraction(
                    req.getHumanAuthorId() != null
                            ? req.getHumanAuthorId() : req.getAuthorId(),
                    "Bot#" + req.getAuthorId(),
                    postId.toString());
        } else {
            viralityService.updateViralityScore(postId, "HUMAN_COMMENT");
        }
        Comment c = new Comment();
        c.setPostId(postId);
        c.setAuthorId(req.getAuthorId());
        c.setAuthorType(authorType);
        c.setContent(req.getContent());
        c.setDepthLevel(req.getDepthLevel());
        return commentRepository.save(c);
    }
    // like
    public void likePost(Long postId, LikePost req) {
        postRepository.findById(postId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        viralityService.updateViralityScore(postId, "HUMAN_LIKE");
        System.out.println("[LIKE] User " + req.getUserId()
                + " liked post " + postId
                + " | Virality: " + viralityService.getViralityScore(postId));
    }
}
