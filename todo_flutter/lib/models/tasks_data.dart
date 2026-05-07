import 'package:flutter/material.dart';
import 'package:todo_flutter/Services/database_services.dart';
import 'package:todo_flutter/models/task.dart';

class TasksData extends ChangeNotifier{
  
  List<Task> tasks = [];

  void addTask(String taskTitle) async {
    Task task = await DatabaseServices.addTask(taskTitle);
    tasks.add(task);
    notifyListeners();
  }

  void updateTask(Task task) async {
    task.toggle();
    await DatabaseServices.updateTask(task);
    notifyListeners();
  }

  void deleteTask(Task task) async {
    await DatabaseServices.deleteTask(task);
    tasks.remove(task);
    notifyListeners();
  }
}