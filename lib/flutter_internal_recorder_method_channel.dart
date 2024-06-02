import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_internal_recorder_platform_interface.dart';

/// An implementation of [FlutterInternalRecorderPlatform] that uses method channels.
class MethodChannelFlutterInternalRecorder extends FlutterInternalRecorderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_internal_recorder');

  /// Stream controller to manage the audio data stream.
  @override
  StreamController<List<int>>? stream;

  MethodChannelFlutterInternalRecorder() {
    methodChannel.setMethodCallHandler((call) async {
      if (call.method == 'onBroadcast') {
        final data = call.arguments as List<int>;

        if (stream != null && !stream!.isClosed) {
          stream!.add(data);
        }
      }
    });
  }

  @override
  Future<bool?> isStreaming() async {
    return await methodChannel.invokeMethod<bool>('isStreaming');
  }

  @override
  Future<bool?> startStreaming({
    int sampleRate = 44100, int bufferSize = 1024
  }) async {
    stream = StreamController<List<int>>();
    
    return await methodChannel.invokeMethod<bool?>('startStreaming', {
      'sample_rate': sampleRate,
      'buffer_size': bufferSize
    });
  }

  @override
  Future<bool?> stopStreaming() async {
    if (stream != null) {
      stream!.close();
    }

    return await methodChannel.invokeMethod<bool?>('stopStreaming');
  }
  
}