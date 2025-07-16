package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name= "comment")
@Data
public class Comment {

    // 댓글 id
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent; //답글의 부모(댓글)

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Comment> replies = new ArrayList<>();

    private String content;

    private Boolean isDeleted = false;

    private Integer likeCount = 0; //단순 댓글 좋아요 수치만 필요

    private Integer replyCount = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public static Comment createComment(Post post, User user, String content) {
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setIsDeleted(false);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        return comment;
    }

    public static Comment createReply(Comment parent, User user, String content) {
        Comment reply = createComment(parent.getPost(), user, content);
        reply.setParent(parent);
        parent.setReplyCount(parent.getReplyCount() + 1);
        return reply;
    }

    public void softDeleteRecursive() {
        this.setIsDeleted(true);
        this.setContent(null);
        for (Comment reply : replies) {
            reply.softDeleteRecursive();
        }
    }
}
