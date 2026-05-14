import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:todo_flutter/Services/auth_service.dart';
import 'package:todo_flutter/Services/globals.dart';
import 'package:todo_flutter/models/task.dart';

class DatabaseServices {
  static Future<Task> addTask(String title) async {

    Map data = {
      'title': title,
    };

    var body = json.encode(data);
    var url = Uri.parse('$tasksBaseUrl/add');
    final headers = await AuthService.authHeaders();

    http.Response response = await http.post(
      url,
      headers: headers,
      body: body,
    );

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception("Failed to add task: ${response.statusCode}");
    }

    print(response.body);

    Map responseBody = jsonDecode(response.body);

    Task task = Task.fromMap(responseBody);

    return task;
  }

  static Future<void> updateTask(Task task) async {
    var url = Uri.parse('$tasksBaseUrl/update/${task.id}');
    final headers = await AuthService.authHeaders();

    final response = await http.put(
      url,
      headers: headers,
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception("Failed to update task: ${response.statusCode}");
    }
  }

  static Future<void> deleteTask(Task task) async {
    var url = Uri.parse('$tasksBaseUrl/delete/${task.id}');
    final headers = await AuthService.authHeaders();

    final response = await http.delete(
      url,
      headers: headers,
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception("Failed to delete task: ${response.statusCode}");
    }
  }

  static Future<List<Task>> getTasks() async {
    var url = Uri.parse(tasksBaseUrl);
    final headers = await AuthService.authHeaders();

    http.Response response = await http.get(
      url,
      headers: headers,
    );

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception("Failed to fetch tasks: ${response.statusCode}");
    }

    print(response.body);

    List responseBody = jsonDecode(response.body);

    List<Task> tasks = [];

    for(Map taskMap in responseBody){
        Task task = Task.fromMap(taskMap);
        tasks.add(task);
    }


    return tasks;
  }
}