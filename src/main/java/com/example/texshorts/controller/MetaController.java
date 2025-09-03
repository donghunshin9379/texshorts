package com.example.texshorts.controller;

import com.example.texshorts.service.CommentService;
import com.example.texshorts.service.PostReactionService;
import com.example.texshorts.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostReactionService postReactionService;


}
