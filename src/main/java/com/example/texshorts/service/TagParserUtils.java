package com.example.texshorts.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TagParserUtils {
    // "#서울 #여행 #카페" → ["서울", "여행", "카페"]
    public static List<String> parseTagsToList(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) return Collections.emptyList();

        return Arrays.stream(rawTags.split("\\s+"))
                .map(String::trim)
                .filter(tag -> tag.startsWith("#"))
                .map(tag -> tag.substring(1)) // remove #
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // ["서울", "여행", "카페"] → "#서울 #여행 #카페"
    public static String formatTagsWithHash(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) return "";

        return tagList.stream()
                .map(tag -> "#" + tag.trim())
                .distinct()
                .collect(Collectors.joining(" "));
    }
}
