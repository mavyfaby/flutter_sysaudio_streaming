import 'dart:async';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_sysaudio_streaming_method_channel.dart';

abstract class FlutterSysAudioStreamingPlatform extends PlatformInterface {
  /// Constructs a FlutterSysAudioStreamingPlatform.
  FlutterSysAudioStreamingPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterSysAudioStreamingPlatform _instance = MethodChannelFlutterSysAudioStreaming();

  /// The default instance of [FlutterSysAudioStreamingPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterSysAudioStreaming].
  static FlutterSysAudioStreamingPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterSysAudioStreamingPlatform] when
  /// they register themselves.
  static set instance(FlutterSysAudioStreamingPlatform instance) {
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