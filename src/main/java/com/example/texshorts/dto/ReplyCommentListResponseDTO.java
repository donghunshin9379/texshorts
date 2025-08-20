package com.example.texshorts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyCommentListResponseDTO {
    private List<CommentResponseDTO> replies;
    private Long lastReplyId;

    //lastReplyId를 자동계산
    public ReplyCommentListResponseDTO(List<CommentResponseDTO> replies) {
        this.replies = replies;
        this.lastReplyId = replies.isEmpty() ? null : replies.get(replies.size() - 1).getId();
    }
}
