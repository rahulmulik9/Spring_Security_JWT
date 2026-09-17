package com.rahul.taskmanager.service;

import com.rahul.taskmanager.dto.TaskRequest;
import com.rahul.taskmanager.dto.TaskResponse;
import com.rahul.taskmanager.entity.Task;
import com.rahul.taskmanager.entity.User;
import com.rahul.taskmanager.exception.TaskNotFoundException;
import com.rahul.taskmanager.repository.TaskRepository;
import com.rahul.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskResponse createTask(TaskRequest request) {
        User currentUser = getCurrentUser();

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .completed(request.isCompleted())
                .owner(currentUser)
                .build();
        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
        return toResponse(task);
    }

    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwner(#id, authentication.name)")
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setCompleted(request.isCompleted());

        Task updated = taskRepository.save(task);
        return toResponse(updated);
    }

    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwner(#id, authentication.name)")
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: " + username));
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .completed(task.isCompleted())
                .createdAt(task.getCreatedAt())
                .build();
    }
}