import 'package:flutter/material.dart';
import 'package:todo_flutter/Services/auth_service.dart';
import 'package:todo_flutter/Screens/home_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({Key? key}) : super(key: key);

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _firstNameController = TextEditingController();
  final TextEditingController _lastNameController = TextEditingController();
  bool _isLoading = false;
  String? _errorMessage;
  bool _useKratos = false;

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    _firstNameController.dispose();
    _lastNameController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      if (_useKratos) {
        await AuthService.loginKratos(
          _usernameController.text.trim(),
          _passwordController.text.trim(),
        );
      } else {
        await AuthService.login(
          _usernameController.text.trim(),
          _passwordController.text.trim(),
        );
      }
      if (!mounted) {
        return;
      }
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
        (route) => false,
      );
    } catch (error) {
      setState(() {
        _errorMessage = _useKratos
            ? "Kratos login failed. Check your credentials."
            : "Login failed. Check your credentials.";
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _registerKratos() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await AuthService.registerKratos(
        email: _usernameController.text.trim(),
        password: _passwordController.text.trim(),
        firstName: _firstNameController.text.trim(),
        lastName: _lastNameController.text.trim(),
      );
      if (!mounted) {
        return;
      }
      setState(() {
        _errorMessage = "Kratos registration succeeded. Please sign in.";
      });
    } catch (error) {
      setState(() {
        _errorMessage = "Kratos registration failed. Check your details.";
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Login"),
        centerTitle: true,
        backgroundColor: Colors.blueAccent,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text("Use Kratos"),
                Switch(
                  value: _useKratos,
                  onChanged: (value) {
                    setState(() {
                      _useKratos = value;
                      _errorMessage = null;
                    });
                  },
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _usernameController,
              decoration: InputDecoration(
                labelText: _useKratos ? "Email" : "Username",
              ),
              textInputAction: TextInputAction.next,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _passwordController,
              decoration: const InputDecoration(labelText: "Password"),
              obscureText: true,
              onSubmitted: (_) => _login(),
            ),
            if (_useKratos) ...[
              const SizedBox(height: 12),
              TextField(
                controller: _firstNameController,
                decoration: const InputDecoration(labelText: "First name"),
                textInputAction: TextInputAction.next,
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _lastNameController,
                decoration: const InputDecoration(labelText: "Last name"),
                textInputAction: TextInputAction.next,
              ),
            ],
            const SizedBox(height: 20),
            if (_errorMessage != null)
              Text(
                _errorMessage!,
                style: const TextStyle(color: Colors.red),
              ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _login,
                style: ElevatedButton.styleFrom(backgroundColor: Colors.blueAccent),
                child: _isLoading
                    ? const SizedBox(
                        height: 18,
                        width: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text(
                        "Sign In",
                        style: TextStyle(color: Colors.white),
                      ),
              ),
            ),
            if (_useKratos) ...[
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: _isLoading ? null : _registerKratos,
                  child: const Text("Create Kratos Account"),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
