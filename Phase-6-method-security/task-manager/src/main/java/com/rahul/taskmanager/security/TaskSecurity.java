package com.rahul.taskmanager.security;

import com.rahul.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("taskSecurity")
@RequiredArgsConstructor
public class TaskSecurity {

    private final TaskRepository taskRepository;

    public boolean isOwner(Long taskId, String username) {
        return taskRepository.findById(taskId)
                .map(task -> task.getOwner().getUsername().equals(username))
                .orElse(false);
    }
}