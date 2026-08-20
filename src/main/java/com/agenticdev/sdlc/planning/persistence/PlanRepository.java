package com.agenticdev.sdlc.planning.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanRepository extends JpaRepository<PlanRecord, UUID> {}
