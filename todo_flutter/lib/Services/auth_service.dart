import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:todo_flutter/Services/globals.dart';
import 'package:todo_flutter/Services/kratos_http_client_stub.dart'
  if (dart.library.html) 'package:todo_flutter/Services/kratos_http_client_web.dart';

class AuthService {
  static const String _jwtTokenKey = "auth_token";
  static const String _kratosSessionTokenKey = "kratos_session_token";

  static Future<String?> getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_jwtTokenKey);
  }

  static Future<String?> getKratosSessionToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_kratosSessionTokenKey);
  }

  static Future<bool> hasToken() async {
    return (await getToken()) != null;
  }

  static Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_jwtTokenKey);
    await prefs.remove(_kratosSessionTokenKey);
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
    await prefs.setString(_jwtTokenKey, token);

    return token;
  }

  static Future<String> loginKratos(String email, String password) async {
    final client = createKratosClient();
    try {
      final Map<String, dynamic> flowBody = await _initKratosFlow(
        client: client,
        flowType: "login",
        refresh: true,
      );
      final String flowId = flowBody["id"] as String;
      final String csrfToken = _extractCsrfToken(flowBody);

      final Map<String, dynamic> payload = {
        "method": "password",
        "identifier": email,
        "password": password,
      };
      if (csrfToken.isNotEmpty) {
        payload["csrf_token"] = csrfToken;
      }

      final loginResponse = await client.post(
        Uri.parse("$kratosBaseUrl/self-service/login?flow=$flowId"),
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json",
        },
        body: json.encode(payload),
      );

      if (loginResponse.statusCode < 200 || loginResponse.statusCode >= 300) {
        final Map<String, dynamic> errorBody =
            jsonDecode(loginResponse.body) as Map<String, dynamic>;
        final Map<String, dynamic>? error =
            errorBody["error"] as Map<String, dynamic>?;
        if (error != null && error["id"] == "session_already_available") {
          return "";
        }
        throw Exception("Kratos login failed: ${loginResponse.statusCode}");
      }

      final Map<String, dynamic> loginBody =
          jsonDecode(loginResponse.body) as Map<String, dynamic>;
      final String sessionToken = (loginBody["session_token"] as String?) ?? "";

      final prefs = await SharedPreferences.getInstance();
      if (sessionToken.isNotEmpty) {
        await prefs.setString(_kratosSessionTokenKey, sessionToken);
      }

      return sessionToken;
    } finally {
      client.close();
    }
  }

  static Future<String> registerKratos({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    final client = createKratosClient();
    try {
      final Map<String, dynamic> flowBody = await _initKratosFlow(
        client: client,
        flowType: "registration",
      );
      final String flowId = flowBody["id"] as String;
      final String csrfToken = _extractCsrfToken(flowBody);

      final Map<String, dynamic> payload = {
        "method": "password",
        "password": password,
        "traits": {
          "email": email,
          "name": {"first": firstName, "last": lastName},
        },
      };
      if (csrfToken.isNotEmpty) {
        payload["csrf_token"] = csrfToken;
      }

      final registerResponse = await client.post(
        Uri.parse("$kratosBaseUrl/self-service/registration?flow=$flowId"),
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json",
        },
        body: json.encode(payload),
      );

      if (registerResponse.statusCode < 200 ||
          registerResponse.statusCode >= 300) {
        throw Exception(
          "Kratos registration failed: ${registerResponse.statusCode}",
        );
      }

      final Map<String, dynamic> registerBody =
          jsonDecode(registerResponse.body);
      final Map<String, dynamic> identity =
          registerBody["identity"] as Map<String, dynamic>;
      return identity["id"] as String;
    } finally {
      client.close();
    }
  }

  static Future<Map<String, dynamic>> _initKratosFlow({
    required http.Client client,
    required String flowType,
    bool refresh = false,
  }) async {
    if (kIsWeb) {
      final String refreshParam = refresh ? "?refresh=true" : "";
      final response = await client.get(
        Uri.parse("$kratosBaseUrl/self-service/$flowType/browser$refreshParam"),
        headers: {"Accept": "application/json"},
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }

      final location = response.headers["location"] ?? "";
      if (location.isNotEmpty) {
        final uri = Uri.parse(location);
        final flowId = uri.queryParameters["flow"];
        if (flowId != null && flowId.isNotEmpty) {
          final flowResponse = await client.get(
            Uri.parse(
              "$kratosBaseUrl/self-service/$flowType/flows?id=$flowId",
            ),
            headers: {"Accept": "application/json"},
          );
          if (flowResponse.statusCode == 200) {
            return jsonDecode(flowResponse.body) as Map<String, dynamic>;
          }
        }
      }

      throw Exception("Kratos $flowType flow failed: ${response.statusCode}");
    }

    final String refreshParam = refresh ? "?refresh=true" : "";
    final response = await client.get(
      Uri.parse("$kratosBaseUrl/self-service/$flowType/api$refreshParam"),
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception("Kratos $flowType flow failed: ${response.statusCode}");
    }
    return jsonDecode(response.body) as Map<String, dynamic>;
  }

  static String _extractCsrfToken(Map<String, dynamic> flowBody) {
    final Map<String, dynamic>? ui = flowBody["ui"] as Map<String, dynamic>?;
    final List<dynamic>? nodes = ui?["nodes"] as List<dynamic>?;
    if (nodes == null) {
      return "";
    }

    for (final node in nodes) {
      final attributes = node["attributes"] as Map<String, dynamic>?;
      if (attributes == null) {
        continue;
      }
      if (attributes["name"] == "csrf_token") {
        return (attributes["value"] as String?) ?? "";
      }
    }
    return "";
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
