package com.aayushxrj.Todo.App.TodosList;

import com.aayushxrj.Todo.App.Task.TaskItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "todos-list", url = "${todoslist.base-url}")
public interface TodosListClient {

    @GetMapping("/tasks")
    List<TaskItem> getTasks();

}