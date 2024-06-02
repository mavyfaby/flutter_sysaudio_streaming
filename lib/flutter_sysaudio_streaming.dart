
import 'flutter_sysaudio_streaming_platform_interface.dart';

class FlutterSysAudioStreaming {
  static Stream<List<int>>? get stream {
    return FlutterSysAudioStreamingPlatform.instance.stream?.stream;
  }

  static Future<bool?> startStreaming({
    int sampleRate = 44100, int bufferSize = 1024
  }) {
    return FlutterSysAudioStreamingPlatform.instance.startStreaming(
      sampleRate: sampleRate,
      bufferSize: bufferSize
    );
  }

  static Future<bool?> stopStreaming() {
    return FlutterSysAudioStreamingPlatform.instance.stopStreaming();
  }

  static Future<bool?> isStreaming() {
    return FlutterSysAudioStreamingPlatform.instance.isStreaming();
  }
}