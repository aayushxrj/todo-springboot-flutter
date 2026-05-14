package com.aayushxrj.Todo.App.Task;

import java.util.List;

public interface TaskService {

    List<TaskItem> getTasks();

    TaskItem addTask(TaskItem taskItem);

    /**
     * Toggle the done state of a task.
     * @param id task id
     * @return true if the task existed and was toggled, false otherwise
     */
    boolean toggleTaskDone(Long id);

    /**
     * Delete a task by id.
     * @param id task id
     * @return true if the task existed and was deleted, false otherwise
     */
    boolean deleteTask(Long id);
}

