import 'dart:developer';
import 'package:flutter/material.dart';
import 'package:flutter_internal_recorder/flutter_internal_recorder.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _isStreaming = false;

  @override
  void initState() {

    FlutterInternalRecorder.isStreaming().then((value) {
      setState(() {
        _isStreaming = value ?? false;
      });
    });
    
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Flutter Internal Recorder'),
        ),
        body: Center(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              FilledButton(
                onPressed: _isStreaming ? null : () async {
                  final result = await FlutterInternalRecorder.startStreaming();
                  log("START: $result");
          
                  if (result != null && result) {
                    FlutterInternalRecorder.stream!.listen((data) {
                      log("DATA: ${data.length}");
                    });
                  }
          
                  setState(() {
                    _isStreaming = result ?? false;
                  });
                },
                child: const Text("Start Recording")
              ),
              const SizedBox(width: 16),
              FilledButton.tonal(
                onPressed: !_isStreaming ? null : () async {
                  final result = await FlutterInternalRecorder.stopStreaming();
                  log("STOP: $result");
          
                  setState(() {
                    _isStreaming = false;
                  });
                },
                child: const Text("Stop Recording")
              ),
            ],
          )
        ),
      ),
    );
  }
}