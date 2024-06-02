package me.mavyfaby.flutter_internal_recorder;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import io.flutter.Log;


/**
 * FlutterInternalRecorderService
 * @author mavyfaby (Maverick Fabroa)
 * @references https://developer.android.com/media/platform/av-capture
 */
public class FlutterInternalRecorderService extends Service {
    private static final String TAG = "FlutterInternalRecorderService";

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private FileOutputStream fos;
    private Thread thread;

    public static boolean isStreaming = false;

    public static final String NOTIFICATION_ID = "FlutterInternalRecorderService:Channel";
    public static final String STREAM_START = "FlutterInternalRecorderService:START";
    public static final String STREAM_STOP = "FlutterInternalRecorderService:STOP";
    public static final String EXTRA_RESULT_DATA = "FlutterInternalRecorderService:Extra:ResultData";
    public static final String EXTRA_BUFFER_SIZE = "FlutterInternalRecorderService:Extra:BufferSize";
    public static final String EXTRA_SAMPLE_RATE = "FlutterInternalRecorderService:Extra:SampleRate";
    public static final String AUDIO_CHUNK = "AUDIO_CHUNK";
    public static final String AUDIO_CHUNK_ID = "AUDIO_CHUNK_ID";

    private static final int SERVICE_ID = 200;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel
        createNotificationChannel();
        // Start service in foreground
        startForeground(SERVICE_ID, new NotificationCompat.Builder(this, NOTIFICATION_ID).build());
        
        // Get media projection manager context
        mediaProjectionManager = (MediaProjectionManager) getApplication()
            .getApplicationContext()
            .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    /**
     * Create notification channel
     */
    private void createNotificationChannel() {
        // Check if device is running on Android Oreo or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Create notification channel class
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_ID, "System Audio Recording",
                NotificationManager.IMPORTANCE_DEFAULT
            );

            // Get notification manager
            NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
            // Create notification channel
            manager.createNotificationChannel(channel);
        }
        
        else {
            Log.e(TAG, "Notification channel not supported on this device");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If intent is null or action is null
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        // If starting stream
        if (intent.getAction().equals(STREAM_START)) {
            // If already recording
            if (isStreaming) {
                // Log already recording
                Log.d(TAG, "Already recording");
                // Return not sticky
                return START_NOT_STICKY;
            }
            
            // Get media projection
            mediaProjection = (MediaProjection) mediaProjectionManager.getMediaProjection(
                Activity.RESULT_OK,
                Objects.requireNonNull(intent.getParcelableExtra(EXTRA_RESULT_DATA))
            );

            // Get buffer size
            int bufferSize = intent.getIntExtra(EXTRA_BUFFER_SIZE, 1024);
            // Get sample rate
            int sampleRate = intent.getIntExtra(EXTRA_SAMPLE_RATE, 44100);
            // Start audio stream
            startAudioStream(bufferSize, sampleRate);
            
            // Return start sticky
            return START_STICKY;
        }

        // If stopping stream
        if (intent.getAction().equals(STREAM_STOP)) {
            // Log stopping audio stream
            Log.d(TAG, "Stopping audio streaming...");
            // Stop audio stream
            stopAudioStream();

            // Return not sticky
            return START_NOT_STICKY;
        }

        // Throw invalid argument exception
        throw new IllegalArgumentException("Invalid intent action");
    }

    /**
     * Start audio stream
     */
    private void startAudioStream(int bufferSize, int sampleRate) {
        // Check if device is running on Android 10 or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Create audio playback capture configuration
            AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration
                .Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build();

            // Create audio format
            AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .setSampleRate(sampleRate)
                .build();

            // Create audio record
            audioRecord = new AudioRecord.Builder()
                .setAudioFormat(format)
                .setAudioPlaybackCaptureConfig(config)
                .build();

            // Start recording
            audioRecord.startRecording();
            // Log start recording
            Log.d(TAG, "Audio streaming started!");
            // Set flag to true
            isStreaming = true;
            
            // Create new thread
            thread = new Thread(() -> {
                // Create buffer array w/ defined size
                byte[] buffer = new byte[bufferSize];

                // While thread is not interrupted
                while (!thread.isInterrupted()) {
                    // Read audio record chunk
                    audioRecord.read(buffer, 0, bufferSize);

                    // Create intent
                    Intent intent = new Intent(AUDIO_CHUNK_ID);
                    // Put buffer data
                    intent.putExtra(AUDIO_CHUNK, buffer);
                    // Send broadcast
                    sendBroadcast(intent);
                }
            });

            // Start thread
            thread.start();
        }
        
        else {
            // Log error
            Log.e(TAG, "Audio streaming is not supported on this device!");
        }
    }

    /**
     * Stop audio stream
     */
    private void stopAudioStream() {
        // Stop all streaming processes
        if (thread != null) thread.interrupt();
        if (audioRecord != null) audioRecord.stop();
        if (audioRecord != null) audioRecord.release();
        if (mediaProjection != null) mediaProjection.stop();

        audioRecord = null;
        isStreaming = false;
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}