package com.agenticdev.sdlc.github.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PullRequestRepository extends JpaRepository<PullRequestRecord, UUID> {
}
