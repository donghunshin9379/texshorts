package com.example.texshorts.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

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
    @JoinColumn(name = "post_id")
    @ToString.Exclude
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    private Comment parent; //답글의 부모(댓글)

    // 답글 리스트 (자식 댓글들)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Comment> replies = new ArrayList<>();

    private String content;

    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL) /** 댓글 삭제하면 리액션도 cascade */
    private List<CommentReaction> commentReactions = new ArrayList<>();

    private int likeCount = 0;

    private int dislikeCount = 0;

    private int replyCount = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();


    public static Comment createRootComment(Post post, User user, String content) {
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setIsDeleted(false);
        comment.setReplyCount(0);
        comment.setLikeCount(0);
        comment.setDislikeCount(0);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        return comment;
    }

    public static Comment createReply(Comment parent, User user, String content) {
        Comment reply = createRootComment(parent.getPost(), user, content);
        reply.setParent(parent);
        parent.getReplies().add(reply);  // 부모 지정
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
