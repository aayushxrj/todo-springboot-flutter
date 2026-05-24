import 'package:flutter/foundation.dart';

final String apiBaseUrl = kIsWeb
	? "http://localhost:9191"
	: (defaultTargetPlatform == TargetPlatform.android
		? "http://10.0.2.2:9191"
		: "http://localhost:9191");

final String tasksBaseUrl = "$apiBaseUrl/tasks";
final String authBaseUrl = "$apiBaseUrl/auth";

final String kratosBaseUrl = kIsWeb
	? "http://localhost:4433"
	: (defaultTargetPlatform == TargetPlatform.android
		? "http://10.0.2.2:4433"
		: "http://localhost:4433");