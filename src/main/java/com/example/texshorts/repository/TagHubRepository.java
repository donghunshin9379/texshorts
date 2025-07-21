package com.example.texshorts.repository;

import com.example.texshorts.entity.TagHub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagHubRepository extends JpaRepository<TagHub,Long> {

    Optional<TagHub> findByTagName(String tagName);


}
