package com.snakei;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.sensibility_testbed.SensibilityApplication;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by lukas.puehringer@nyu.edu
 * on 5/27/16.
 *
 * A pseudo Service class that facades Android media services
 *
 * Provides methods to start and stop Text-To-Speech engine,
 * synthesize text to speech, record from the default microphone
 * and get information about whether media is being played on the
 * device or if text is synthesized to speech
 *
 * This class is a Singleton using the thread safe
 * Java Initialization on Demand Holder pattern
 * (cf. SensorService.java for more info )
 *
 * Todo:
 *   - Currently we use default settings for text to speech and  audio
 *     recording Should we allow the user to change these settings?
 *
 *   - Same story as in other Sensibility Services, how do we handle
 *     initialization and release of resources?
 *
 *   - Doesn't implement get_media_play_info() from old API anymore,
 *     could be added
 */
public class MediaService  {
    static final String TAG = "MediaService";

    private Context app_context;

    // Used for microphone recording
    private AudioManager audio_manager;

    // Used for text to speech
    private TextToSpeech tts;
    private boolean tts_initialized = false;
    private Object tts_sync;

    // Option to flush tts queue when tts is called
    // Alternative: TextToSpeech.QUEUE_ADD
    private int queue_mode = TextToSpeech.QUEUE_FLUSH;

    /* See Initialization on Demand Holder pattern */
    private static class MediaServiceHolder {
        private static final MediaService instance = new MediaService();
    }

    /* Classic Singleton Instance Getter */
    public static MediaService getInstance() {
        return MediaServiceHolder.instance;
    }

    public MediaService() {
        app_context = SensibilityApplication.getAppContext();
        audio_manager = (AudioManager)app_context.getSystemService(
                Context.AUDIO_SERVICE);
        tts_sync = new Object();
    }


    /*
     * Initializes the text-to-speech engine, sets a boolean to true
     * if it was successfully initialized and notifies waiting functions
     * via the "tts_sync" object
     *
     * Note:
     * While AudioRecorder is initialized and released per recording tts
     * should be initialized before attempting to use tts because it
     * takes "some" time
     *
     */
    public void start_media() {
        tts = new TextToSpeech(app_context,new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    synchronized (tts_sync) {
                        tts_initialized = true;
                        tts_sync.notify();
                    }
                }
            }
        });
    }


    /*
     * Shuts down the tts engine if it has been initialized
     *
     */
    public void stop_media() {
        tts_initialized = false;
        if(tts != null) {
            tts.shutdown();
        }
    }


    /*
     * Records from the default microphone to a file at
     * passed "file_name" for a given "duration" (ms).
     * This is blocking operation.
     *
     * Creates, prepares and starts a new MediaRecorder.
     * Then suspends the calling thread for the specified time
     * (recording is performed on another thread) and releases the
     * resource once it is finished.
     *
     * @param   Path to store recording to (String)
     * @param   Duration of recording (int)
     *
     * Notes:
     * This strategy is preferred over non-blocking, because Android
     * makes it hard to determine the current state of a MediaRecorder
     * which in turn makes proper resource handling (release when finished)
     * difficult.
     *
     * Possible non-blocking approach
     *      For a non blocking approach we could use recording session duration:
     *      recorder.setMaxDuration(int max_duration_ms) or a custom timer
     *      together with info or error listener:
     *          recorder.setOnInfoListener(OnInfoListener) or
     *          recorder.setOnErrorListener(OnErrorListener)
     *
     * There are a lot of things that can be parametrized
     *     e.g. Format, Encoder, ...
     */
    public void microphoneRecord(String file_name, int duration)
            throws InterruptedException, IOException {
        // Create new media recorder and start with default settings
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(file_name);
        recorder.prepare();
        recorder.start();

        // Sleep until recording has finished
        Thread.sleep(duration);

        // Release resources
        recorder.stop();
        recorder.release();
    }

    /*
     * Returns whether music is currently playing by any app on the device
     *
     * @return  Is media playing (boolean)
     *
     * Note:
     * Simple data types are easy to use in calling native code
     * That is why we don't need to return a serialized JSON Object
     */
    public boolean isMediaPlaying() {
        return audio_manager.isMusicActive();
    }


    /*
     * Returns whether text is currently synthesized or
     * in the queue to be synthesised to speech
     *
     * @return  Is text synthesized to speech (boolean)
     *
     * Note:
     * Simple data types are easy to use in calling native code
     * That is why we don't need to return a serialized JSON Object
     */
    public boolean isTtsSpeaking() {
        if (tts_initialized) {
            return tts.isSpeaking();
        }
        return false;
    }

    /*
     * Takes a String and synthesizes it to speech using the tts default engine
     * if it is initialized. If tts engine is being initialized it waits until
     * it gets notified by the engine's onInit callback
     * This is a non-blocking operation, i.e. it might return before text was
     * synthesized
     * Utterance progress can be implemented using
     * android.speech.tts.UtteranceProgressListener
     *
     * Note:
     * Initializing tts engine only once eliminates long initialization
     * time (~8 seconds on developer device) for subsequent calls to ttsSpeak
     *
     * Caveats:
     *  - Have to call start_media before using ttsSpeak and stop_media to
     *    release resources
     *  - Concurrent calls to ttsSpeak will cancel each other
     *
     * Further possible parameters
     *  - QUEUE_ADD - add to tts queue and synthesizes when ready
     *  - QUEUE_FLUSH - flushes queue and plays immediately
     *  - KEY_PARAM_STREAM
     *  - KEY_PARAM_VOLUME
     *  - KEY_PARAM_PAN
     *  - Engine specific parameters
     */
    public void ttsSpeak(String message) throws Throwable {
        if (tts != null) {
            synchronized (tts_sync) {
                if (!tts_initialized) {
                    tts_sync.wait();
                }
                UUID utterance_id = UUID.randomUUID();
                tts.speak(message, queue_mode, null, utterance_id.toString());
            }
        } else {
            throw new Throwable("Text-to-speech engine has not been started.");
        }
    }
}
