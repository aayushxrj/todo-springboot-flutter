import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:todo_flutter/Screens/add_task_screen.dart';
import 'package:todo_flutter/Services/database_services.dart';
import 'package:todo_flutter/models/task.dart';
import 'package:todo_flutter/models/tasks_data.dart';
import 'package:todo_flutter/task_tile.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<Task>? tasks;

  void getTasks() async {
    List<Task> fetchedTasks = await DatabaseServices.getTasks();
    Provider.of<TasksData>(context, listen: false).tasks = fetchedTasks;
    setState(() {
      tasks = fetchedTasks;
    });
  }

  @override
  void initState() {
    super.initState();
   getTasks();
  }
    
  @override
  Widget build(BuildContext context) {
    return tasks == null ?
    Scaffold(
      body: Center(
        child: CircularProgressIndicator(),
      ),
    )
    : Scaffold(
      appBar: AppBar(
        title: Text('Todo Tasks (${Provider.of<TasksData>(context).tasks.length})'),
        centerTitle: true,
        backgroundColor: Colors.blueAccent,
      ),
      body: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        child: Consumer<TasksData>(
          builder: (context, tasksData, child) {
            return ListView.builder(
              itemCount: tasksData.tasks.length,
              itemBuilder: (context, index) {
                Task task = tasksData.tasks[index];
                return TaskTile(
                  task: task,
                  tasksData: tasksData,
                );
              },
            );
          },
          ),
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: Colors.blueAccent,
        child: const Icon(
          Icons.add
        ),
        onPressed: () {
          showModalBottomSheet(
            context: context, 
            builder: (context){
              return const AddTaskScreen();
            });
        },
      ),
      );
  }
}
