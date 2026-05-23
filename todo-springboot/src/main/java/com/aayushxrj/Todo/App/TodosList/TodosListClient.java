package com.aayushxrj.Todo.App.TodosList;

import com.aayushxrj.Todo.App.Task.TaskItem;
import feign.RequestLine;

import java.util.List;

public interface TodosListClient {
	@RequestLine("GET /tasks")
	List<TaskItem> getTasks();
}