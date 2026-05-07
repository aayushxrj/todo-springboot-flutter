import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:todo_flutter/Services/globals.dart';
import 'package:todo_flutter/models/task.dart';

class DatabaseServices {
  static Future<Task> addTask(String title) async {

    Map data = {
      'title': title,
    };

    var body = json.encode(data);
    var url = Uri.parse('$baseURL/add');

    http.Response response = await http.post(
        url,
        headers: headers,
        body: body
    );

    print(response.body);

    Map responseBody = jsonDecode(response.body);

    Task task = Task.fromMap(responseBody);

    return task;
  }

  static Future<void> updateTask(Task task) async {
    var url = Uri.parse('$baseURL/update/${task.id}');

    await http.put(
      url,
      headers: headers,
    );
  }

  static Future<void> deleteTask(Task task) async {
    var url = Uri.parse('$baseURL/delete/${task.id}');

    await http.delete(
      url,
      headers: headers,
    );
  }

  static Future<List<Task>> getTasks() async {
    var url = Uri.parse(baseURL);

    http.Response response = await http.get(
        url,
        headers: headers
    );

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