package com.agenticdev.sdlc.planning.domain;

public record FileChange(String path, ChangeType change, String reason) {
    public enum ChangeType { CREATE, MODIFY, DELETE }
}
