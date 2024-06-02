import 'dart:async';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_internal_recorder_method_channel.dart';

abstract class FlutterInternalRecorderPlatform extends PlatformInterface {
  /// Constructs a FlutterInternalRecorderPlatform.
  FlutterInternalRecorderPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterInternalRecorderPlatform _instance = MethodChannelFlutterInternalRecorder();

  /// The default instance of [FlutterInternalRecorderPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterInternalRecorder].
  static FlutterInternalRecorderPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterInternalRecorderPlatform] when
  /// they register themselves.
  static set instance(FlutterInternalRecorderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  StreamController<List<int>>? get stream {
    throw UnimplementedError('stream has not been implemented.');
  }

  Future<bool?> isStreaming() {
    throw UnimplementedError('isStreaming() has not been implemented.');
  }

  Future<bool?> startStreaming({
    required int sampleRate, required int bufferSize
  }) {
    throw UnimplementedError('startStreaming() has not been implemented.');
  }

  Future<bool?> stopStreaming() {
    throw UnimplementedError('stopStreaming() has not been implemented.');
  }

}