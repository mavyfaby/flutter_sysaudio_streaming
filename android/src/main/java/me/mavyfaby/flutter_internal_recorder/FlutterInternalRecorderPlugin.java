package me.mavyfaby.flutter_internal_recorder;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * FlutterInternalRecorderPlugin
 * @author mavyfaby (Maverick Fabroa)
 * @references https://developer.android.com/media/platform/av-capture
 */
public class FlutterInternalRecorderPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {
  private final String TAG = "FlutterInternalRecorderPlugin";

  private MethodChannel channel;
  private Context context;
  private Activity activity;

  private MediaProjectionManager mediaProjectionManager;
  private MediaProjection mediaProjection;
  private MethodChannel.Result result;

  private AudioFormat format;
  private AudioRecord recorder;
  private AudioPlaybackCaptureConfiguration config;

  private static final int MEDIA_PROJECTION_REQUEST_CODE = 1;
  private static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 2;

  private static Integer bufferSize = 0;
  private static Integer sampleRate = 0;
  private static boolean isRequesting = false;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    // Set up MethodChannel
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_internal_recorder");
    channel.setMethodCallHandler(this);
    // Get native application context
    context = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    // Get activity
    this.activity = binding.getActivity();
    // Add activity result and permission listeners
    binding.addActivityResultListener(this);
    binding.addRequestPermissionsResultListener(this);
    // Register stream receiver
    context.registerReceiver(audioStreamReceiver(), new IntentFilter(FlutterInternalRecorderService.AUDIO_CHUNK_ID));
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    // Set global result object
    this.result = result;

    // Check method call from Flutter app
    switch (call.method) {
      // If calling startStreaming
      case "startStreaming":
        try {
          // Get sample rate from call arguments
          sampleRate = call.argument("sample_rate");
          // Get buffer size from call arguments
          bufferSize = call.argument("buffer_size");

          // Set default sample rate if null
          if (sampleRate == null) {
            sampleRate = 44100; // 44.1 kHz
          }

          // Set default buffer size if null
          if (bufferSize == null) {
            bufferSize = 1024; // 1024 bytes
          }

          // Log request data
          Log.d(TAG, "Sample Rate: " + sampleRate + " Hz" + " Buffer Size: " + bufferSize + " bytes");
          // Start streaming
          startStreaming();
        }
        
        catch (Exception e) {
          // Log error
          Log.e(TAG, e.toString());
          // Return false result
          result.success(false);
        }

        break;

      // If calling stopStreaming
      case "stopStreaming":
        try {
          // Stop streaming
          stopStreaming();
          // Return null result
          result.success(true);
        }
        
        catch (Exception e) {
          // Log error message
          Log.e(TAG, e.toString());
          // Return false result
          result.success(false);
        }

        break;

      // If calling isStreaming
      case "isStreaming":
        // Return true if currently recording
        result.success(FlutterInternalRecorderService.isStreaming);
        break;

      default:
        result.notImplemented();
    }
  }

  /**
   * Request permissions and start streaming
   */
  private void startStreaming() {
    // Log start streaming
    Log.d(TAG, "Start streaming!");

    // Check if record audio permission is granted
    if (!isRecordAudioPermissionGranted()) {
      requestRecordAudioPermission();
      return;
    }
    
    // Otherwise, start media projection request
    startMediaProjectionRequest();
  }

  /**
   * Stop streaming
   */
  private void stopStreaming() {
    // Log stop streaming
    Log.d(TAG, "Stop streaming!");

    // Initialize FlutterInternalRecorderService intent with STREAM_STOP
    Intent intent = new Intent(context, FlutterInternalRecorderService.class);
    intent.setAction(FlutterInternalRecorderService.STREAM_STOP);

    // Start foreground service
    activity.startService(intent);
  }

  /**
   * Audio chunk receiver
   */
  private BroadcastReceiver audioStreamReceiver() {
    // Return broadcast receiver
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        // Get audio data
        byte[] data = intent.getByteArrayExtra(FlutterInternalRecorderService.AUDIO_CHUNK);

        // Check if data is not null
        if (data != null) {
          // Send data to Flutter app
          channel.invokeMethod("onBroadcast", data);
        }
      }
    };
  }

  /**
   * Request record audio permission
   */
  private void requestRecordAudioPermission() {
    // List of permissions
    String[] permissions = { Manifest.permission.RECORD_AUDIO };
    // Request permission
    ActivityCompat.requestPermissions(activity, permissions, RECORD_AUDIO_PERMISSION_REQUEST_CODE);
  }


  /**
   * Request MediaProjection permission and start streaming
   */
  private void startMediaProjectionRequest() {
    // If already requesting, log message
    if (isRequesting) {
      // Log already requesting
      Log.d(TAG, "Already requesting MediaProjection permission!");
      // Stop function execution
      return;
    }

    // Log start MediaProjectionRequest
    Log.d(TAG, "Requesting MediaProjection permission!");
    // Set requesting flag to true
    isRequesting = true;

    // Initialize MediaProjectionManager
    mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    // Create screen capture intent
    Intent intent = mediaProjectionManager.createScreenCaptureIntent();
    // Start activity for result
    activity.startActivityForResult(intent, MEDIA_PROJECTION_REQUEST_CODE);
  }
  

  /**
   * Check if record audio permission is granted
   */
  private boolean isRecordAudioPermissionGranted() {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    // Set requesting flag to false
    isRequesting = false;

    // Check if requestCode is MEDIA_PROJECTION_REQUEST_CODE
    if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
    // Check if resultCode is Activity.RESULT_OK
      if (resultCode == Activity.RESULT_OK) {
        // Initialize FlutterInternalRecorderService intent
        Intent intent = new Intent(context, FlutterInternalRecorderService.class);

        // Set intent action to STREAM_START
        intent.setAction(FlutterInternalRecorderService.STREAM_START);
        intent.putExtra(FlutterInternalRecorderService.EXTRA_BUFFER_SIZE, bufferSize);
        intent.putExtra(FlutterInternalRecorderService.EXTRA_SAMPLE_RATE, sampleRate);
        intent.putExtra(FlutterInternalRecorderService.EXTRA_RESULT_DATA, data);

        // Start foreground service in a new thread
        new Thread(() -> {
            // Start foreground service
            activity.startForegroundService(intent);
            // Log starting service
            Log.d(TAG, "Foreground Service Started!");
        })
        // Start thread
        .start();

        // Log media projection result
        Log.d(TAG, "MediaProjection permission granted!");
        // Return true result
        result.success(true);
        // Return true
        return true;
      }
      
      else {
        // Log media projection result
        Log.d(TAG, "MediaProjection permission denied!");
        // Return false result
        result.success(false);
      }

    }

    return false;
  }
  
  @Override
  public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // Check if requestCode is RECORD_AUDIO_PERMISSION_REQUEST_CODE
    if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
      // Check if granted
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Log record audio permission granted
        Log.d(TAG, "RECORD AUDIO PERMISSION GRANTED");
        // Show toast message
        Toast.makeText(context, "Record audio permission granted!", Toast.LENGTH_SHORT).show();
      }
      
      else {
        // Log record audio permission denied
        Log.d(TAG, "RECORD AUDIO PERMISSION DENIED");
        // Show toast message
        Toast.makeText(context, "Record audio permission denied!", Toast.LENGTH_SHORT).show();
      }
    }

    return true;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    // Reattach activity
    this.activity = (FlutterActivity) binding.getActivity();
    // Reset activity result and permission listeners
    binding.addActivityResultListener(this);
    binding.addRequestPermissionsResultListener(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onDetachedFromActivity() {
    // Leave empty for now
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    // Leave empty for now
  }
}