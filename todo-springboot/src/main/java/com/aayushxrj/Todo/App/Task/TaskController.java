package com.aayushxrj.Todo.App.Task;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aayushxrj.Todo.App.Permify.PermifyAuthorizationService;

import java.util.List;

@RestController
@RequestMapping(path = "/tasks")
@CrossOrigin
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private PermifyAuthorizationService permifyAuthorizationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public List<TaskItem> getTasks(){
        return taskService.getTasks();
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public TaskItem addTask(@Valid @RequestBody TaskItem taskItem) {
        if (!hasRole("ROLE_ADMIN") && !hasRole("ROLE_USER")) {
            permifyAuthorizationService.requireSystemPermission("create_tasks");
        }
        return taskService.addTask(taskItem);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity updateTask(@PathVariable Long id){
        if (!hasRole("ROLE_ADMIN")) {
            permifyAuthorizationService.requireTaskPermission("update", id.toString());
        }
        boolean updated = taskService.toggleTaskDone(id);
        if(updated){
            return new ResponseEntity<>("Task is updated", HttpStatus.OK);
        }
        return new ResponseEntity<>("Task is not exist", HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity deleteTask(@PathVariable Long id){
        if (!hasRole("ROLE_ADMIN")) {
            permifyAuthorizationService.requireTaskPermission("delete", id.toString());
        }
        boolean deleted = taskService.deleteTask(id);
        if(deleted){
            return new ResponseEntity<>("Task is deleted", HttpStatus.OK);
        }
        return new ResponseEntity<>("Task is not exist", HttpStatus.BAD_REQUEST);
    }

    private boolean hasRole(String role) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return false;
        }
        for (GrantedAuthority authority : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

}
