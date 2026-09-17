package com.rahul.taskmanager.controller;

import com.rahul.taskmanager.dto.TaskResponse;
import com.rahul.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TaskService taskService;

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getAllTasksAsAdmin() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }
}