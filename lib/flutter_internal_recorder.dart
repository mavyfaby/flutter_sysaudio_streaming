
import 'flutter_internal_recorder_platform_interface.dart';

class FlutterInternalRecorder {
  static Stream<List<int>>? get stream {
    return FlutterInternalRecorderPlatform.instance.stream?.stream;
  }

  static Future<bool?> startStreaming({
    int sampleRate = 44100, int bufferSize = 1024
  }) {
    return FlutterInternalRecorderPlatform.instance.startStreaming(
      sampleRate: sampleRate,
      bufferSize: bufferSize
    );
  }

  static Future<bool?> stopStreaming() {
    return FlutterInternalRecorderPlatform.instance.stopStreaming();
  }

  static Future<bool?> isStreaming() {
    return FlutterInternalRecorderPlatform.instance.isStreaming();
  }
}