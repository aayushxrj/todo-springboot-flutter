import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:todo_flutter/Services/globals.dart';

class AuthService {
  static const String _tokenKey = "auth_token";

  static Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  static Future<bool> hasToken() async {
    return (await getToken()) != null;
  }

  static Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
  }

  static Future<String> login(String username, String password) async {
    final url = Uri.parse("$authBaseUrl/login");
    final body = json.encode({"username": username, "password": password});

    final response = await http.post(
      url,
      headers: {"Content-Type": "application/json"},
      body: body,
    );

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception("Login failed: ${response.statusCode}");
    }

    final Map<String, dynamic> responseBody = jsonDecode(response.body);
    final String token = responseBody["token"] as String;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);

    return token;
  }

  static Future<Map<String, String>> authHeaders() async {
    final token = await getToken();
    final headers = <String, String>{"Content-Type": "application/json"};
    if (token != null && token.isNotEmpty) {
      headers["Authorization"] = "Bearer $token";
    }
    return headers;
  }
}
