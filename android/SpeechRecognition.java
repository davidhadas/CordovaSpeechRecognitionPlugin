package org.apache.cordova.speech;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;

import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;


public class SpeechRecognition extends CordovaPlugin {

  private static final String TAG = XSpeechRecognizer.class.getSimpleName();
  public static final String ACTION_INIT = "init";
  public static final String ACTION_START = "start";
  public static final String ACTION_STOP = "stop";

  private CallbackContext callbackContext;
  private LanguageDetailsChecker languageDetailsChecker;
  private SpeechRecognizer recognizer;
  private boolean recognizerPresent = false;
  private Handler loopHandler;

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    this.callbackContext = callbackContext;

    if (ACTION_START.equals(action)) {
      if (!recognizerPresent) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
      }
      
      startSpeechRecognitionActivity(args);     
    } else if (ACTION_INIT.equals(action)) {
      this.recognizerPresent = SpeechRecognizer.isRecognitionAvailable(this.cordova.getActivity().getBaseContext());
      if (!this.recognizerPresent) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
        return true; // TBD should we return false?
      }          
      recognizer = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity().getBaseContext());
      loopHandler = new Handler(Looper.getMainLooper());
      callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
      
      loopHandler.post(new Runnable() {
        @Override
        public void run() {
          recognizer.setRecognitionListener(new SpeechRecognitionListner());
        }              
      });
    } else if(ACTION_STOP.equals(action)) {
      loopHandler.post(new Runnable() {
        @Override
        public void run() {
            recognizer.stopListening();
        }
      });
    } else {
      this.callbackContext.error("Unknown action: " + action);
      return false;
    }
    return true;
  }

    private void fireRecognitionEvent(ArrayList<String> transcripts, float[] confidences) {
        JSONObject event = new JSONObject();
        JSONArray results = new JSONArray();
        try {
            for(int i=0; i<transcripts.size(); i++) {
                JSONArray alternatives = new JSONArray();
                JSONObject result = new JSONObject();
                result.put("transcript", transcripts.get(i));
                result.put("final", true);
                if (confidences != null) {
                    result.put("confidence", confidences[i]);
                }
                alternatives.put(result);
                results.put(alternatives);
            }
            event.put("type", "result");
            // event.put("emma", null);
            // event.put("interpretation", null);
            event.put("results", results);
        } catch (JSONException e) {
            // this will never happen
        }
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pr); 
    }

    private void fireErrorEvent(Integer code){
        JSONObject event = new JSONObject();
        try {
            event.put("type", "error");
            event.put("code", code.toString());
        } catch (JSONException e) {
            // this will never happen
        }

        PluginResult pr = new PluginResult(PluginResult.Status.ERROR, event);
        pr.setKeepCallback(false);
        this.callbackContext.sendPluginResult(pr); 
    }

    private void fireEvent(String type) {
        // callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Event"));
        JSONObject event = new JSONObject();
        try {
            event.put("type",type);
        } catch (JSONException e) {
            // this will never happen
        }
        // PluginResult pr = new PluginResult(PluginResult.Status.OK, "event");
        PluginResult pr = new PluginResult(PluginResult.Status.OK, event);
        pr.setKeepCallback(true);
        this.callbackContext.sendPluginResult(pr); 
    }

    private void stopSpeechRecognitionActivity(){
        Handler loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {

            @Override
            public void run() {
                recognizer.stopListening();
                // recognizer.cancel();
            }
            
        });
    }

    /**
     * Fire an intent to start the speech recognition activity.
     *
     * @param args Argument array with the following string args: [req code][number of matches]
     */
    private void startSpeechRecognitionActivity(JSONArray args) {

        int maxMatches = 0;
        String language = Locale.getDefault().toString();

        try {
            if (args.length() > 0) {
                // Maximum number of matches, 0 means the recognizer decides
                String temp = args.getString(0);
                maxMatches = Integer.parseInt(temp);
            }
            if (args.length() > 1) {
                // Language
                language = args.getString(1);
            }
        }
        catch (Exception e) {
            Log.e(TAG, String.format("startSpeechRecognitionActivity exception: %s", e.toString()));
        }

        // Create the intent and set parameters
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"voice.recognition.test");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);

        if (maxMatches > 0)
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxMatches);

        Handler loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {

            @Override
            public void run() {
                recognizer.startListening(intent);
            }
            
        });
    }
    
    /*
     *  Get the list of supported languages
     */
    private void getSupportedLanguages() {
    	if (languageDetailsChecker == null){
    		languageDetailsChecker = new LanguageDetailsChecker(callbackContext);
    	}
    	Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
    	cordova.getActivity().sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
	}

    class listener implements RecognitionListener          
    {
        public void onReadyForSpeech(Bundle params)
        {
            fireEvent("ready");
            Log.d(TAG, "onReadyForSpeech");
        }
        public void onBeginningOfSpeech()
        {
            fireEvent("start");
            Log.d(TAG, "onBeginningOfSpeech");
        }
        /* RMV Voltage */
        public void onRmsChanged(float rmsdB)
        {
            // fireEvent("rms changed");
            Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            fireEvent("buffer received");
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            fireEvent("end");
            Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error)
        {
            fireErrorEvent(error);
            Log.d(TAG,  "error " +  error);
        }
        public void onResults(Bundle results)                   
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            ArrayList<String> transcript = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            if (transcript.size() > 0) {
                Log.d(TAG, "fire recognition event");
                fireRecognitionEvent(transcript, confidence);
            } else {
                Log.d(TAG, "fire no match event");
                fireEvent("nomatch");
            }  
        }
        public void onPartialResults(Bundle partialResults)
        {
            fireEvent("partial results");
             Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            fireEvent("event");
             Log.d(TAG, "onEvent " + eventType);
        }
    }
}
