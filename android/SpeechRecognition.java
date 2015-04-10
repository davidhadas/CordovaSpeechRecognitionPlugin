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
import android.content.ComponentName;


public class SpeechRecognition extends CordovaPlugin {

  private static final String TAG = SpeechRecognition.class.getSimpleName();
  public static final String ACTION_INIT = "init";
  public static final String ACTION_START = "start";
  public static final String ACTION_STOP = "stop";
    

  private CallbackContext callbackContext;
  private Handler loopHandler;
  public static final String NOT_PRESENT_MESSAGE = "Speech recognition is not present or enabled";
  //private LanguageDetailsChecker languageDetailsChecker;
  private SpeechRecognizer recognizer;
  private boolean recognizerPresent = false;
  private Intent intent;
  private ComponentName recognizerName = new ComponentName("com.google.android.googlequicksearchbox",
                                                           "com.google.android.voicesearch.serviceapi.GoogleRecognitionService");
                                //'com.google.android.voicesearch';    
                                //"com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService"

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    this.callbackContext = callbackContext;

    if (ACTION_START.equals(action)) {
        if (!recognizerPresent) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
            return true;
        }
        loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {
            @Override
            public void run() {
                recognizer.startListening(intent);
            }
        });

    } else if (ACTION_INIT.equals(action)) {
        recognizerPresent = SpeechRecognizer.isRecognitionAvailable(cordova.getActivity().getBaseContext());
        if (!recognizerPresent) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_PRESENT_MESSAGE));
            return true; // TBD should we return false?
        }          
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
      
        loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {
            @Override
            public void run() {
                recognizer = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity().getBaseContext(),recognizerName);
                recognizer.setRecognitionListener(new SpeechRecognitionListner());
            }              
        });
        
        String maxResStr = args.optString(0, "1");
        String lang = args.optString(1, Locale.getDefault().toString());
        int maxRes = Integer.parseInt(maxResStr);
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); 
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH); //RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"org.apache.cordova.speech   ");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxRes); 
        
    } else if(ACTION_STOP.equals(action)) {
        loopHandler.post(new Runnable() {
            @Override
            public void run() {
                recognizer.stopListening();
            }
        });
    } else {
        callbackContext.error("Unknown action: " + action);
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
        callbackContext.sendPluginResult(pr); 
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
        callbackContext.sendPluginResult(pr); 
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
        callbackContext.sendPluginResult(pr); 
    }

    class SpeechRecognitionListner implements RecognitionListener          
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
            //fireEvent("rms changed");
            //Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            //fireEvent("buffer received");
            //Log.d(TAG, "onBufferReceived");
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
            //fireEvent("partial results");
            //Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            //fireEvent("event");
            Log.d(TAG, "onEvent " + eventType);
        }
    }
}
