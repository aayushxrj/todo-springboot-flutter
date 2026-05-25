package com.aayushxrj.Todo.App.Task;

import com.aayushxrj.Todo.App.TodosList.TodosListClient;
import com.aayushxrj.Todo.App.Permify.PermifyAuthorizationService;
import com.aayushxrj.Todo.App.Permify.PermifyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PermifyAuthorizationService permifyAuthorizationService;

    @Autowired
    private PermifyClient permifyClient;

    @Autowired
    private TodosListClient todosListClient;

    @Override
    public List<TaskItem> getTasks() {
        // Temporarily bypass Permify and TodosList to validate Kratos flow.
        // permifyAuthorizationService.requireSystemPermission("read_tasks");
        // return todosListClient.getTasks();
        return taskRepository.findAll();
    }

    @Override
    public TaskItem addTask(TaskItem taskItem) {
        if (!hasRole("ROLE_ADMIN") && !hasRole("ROLE_USER")) {
            permifyAuthorizationService.requireSystemPermission("create_tasks");
        }
        TaskItem saved = taskRepository.save(taskItem);
        permifyClient.writeTuples(
            java.util.List.of(
                Map.of(
                    "entity", Map.of("type", "task", "id", saved.getId().toString()),
                    "relation", "system",
                    "subject", Map.of("type", "system", "id", "root", "relation", "")
                )
            )
        );
        return saved;
    }

    @Override
    public boolean toggleTaskDone(Long id) {
        if (!hasRole("ROLE_ADMIN")) {
            permifyAuthorizationService.requireTaskPermission("update", id.toString());
        }
        return taskRepository.findById(id).map(task -> {
            task.setDone(!task.isDone());
            taskRepository.save(task);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean deleteTask(Long id) {
        if (!hasRole("ROLE_ADMIN")) {
            permifyAuthorizationService.requireTaskPermission("delete", id.toString());
        }
        if(taskRepository.existsById(id)){
            taskRepository.deleteById(id);
            return true;
        }
        return false;
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

