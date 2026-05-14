package com.aayushxrj.Todo.App.Task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Override
    public List<TaskItem> getTasks() {
        return taskRepository.findAll();
    }

    @Override
    public TaskItem addTask(TaskItem taskItem) {
        return taskRepository.save(taskItem);
    }

    @Override
    public boolean toggleTaskDone(Long id) {
        return taskRepository.findById(id).map(task -> {
            task.setDone(!task.isDone());
            taskRepository.save(task);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean deleteTask(Long id) {
        if(taskRepository.existsById(id)){
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

