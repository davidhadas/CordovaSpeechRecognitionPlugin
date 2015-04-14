package com.zLineup.cordova.plugin.speech;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;

import android.content.Context;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.speech.RecognitionService;



public class SpeechRecognition extends CordovaPlugin {

  private static final String TAG = SpeechRecognition.class.getSimpleName();
  public static final String ACTION_INIT = "init";
  public static final String ACTION_CONF = "config";
  public static final String ACTION_START = "start";
  public static final String ACTION_STOP = "stop";
  public static final String ACTION_CANCEL = "cancel";

  public static final int ERR_NO_RECOGNIZER = -1;
  public static final int ERR_RECOGNIZER_UNTESTED = -2;
  public static final int ERR_NOT_STARTED = -3;
  public static final int ERR_FAIL_START = -4;
    

    

  private CallbackContext callbackContext;
  private Handler loopHandler;
  public static final String NOT_PRESENT_MESSAGE = "Speech recognition is not present or enabled";
  //private LanguageDetailsChecker languageDetailsChecker;
  private SpeechRecognizer recognizer;
  private boolean recognizerPresent = false;
  private Intent intent;
  private Context myContext;
  private ComponentName recognizerName;
            //Pkg:  "com.google.android.googlequicksearchbox",
            //Name: "com.google.android.voicesearch.serviceapi.GoogleRecognitionService");

  // Android OS Implementation of recognizer class can be found at: 
  // https://code.google.com/p/pdroid/source/browse/trunk/frameworks/base/core/java/android/speech/SpeechRecognizer.java?r=20    
  //
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    this.callbackContext = callbackContext;

    if (ACTION_START.equals(action)) {
        Log.d(TAG,"SpeechRecognition ACTION_START ");
        if (!recognizerPresent) {
            fireErrorEvent(ERR_NOT_STARTED);    
            return true;
        }

        loopHandler.post(new Runnable() {
            @Override
            public void run() {
                recognizer.startListening(intent);
            }
        });
    } else if (ACTION_INIT.equals(action)) {
        // Note that we are not running in main loop 
        // Log.d(TAG, "SpeechRecognition same: " + (Looper.myLooper() == Looper.getMainLooper()));
        
        Log.d(TAG,"SpeechRecognition ACTION_INIT ");
        if (recognizerPresent) return true; //oneshot
        myContext = cordova.getActivity().getApplicationContext(); // use to be getBaseContext();
        recognizerPresent = SpeechRecognizer.isRecognitionAvailable(myContext);
        if (!recognizerPresent) {
            fireErrorEvent(ERR_FAIL_START);    
            return true;
        }          
        recognizerName = pickRecognizer("com.google.android");
        fireEvent("initialized");
    
        loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {
            @Override
            public void run() {
                if  (recognizerName == null) recognizer = SpeechRecognizer.createSpeechRecognizer(myContext);
                else recognizer = SpeechRecognizer.createSpeechRecognizer(myContext,recognizerName);
                recognizer.setRecognitionListener(new SpeechRecognitionListner());
            }              
        });
    } else if (ACTION_CONF.equals(action)) {
        Log.d(TAG,"SpeechRecognition ACTION_CONF ");
        if (!recognizerPresent) {
            fireErrorEvent(ERR_NOT_STARTED);    
            return true;
        }
        // FIXME - cleanup recognizer et al if already initialized
        
        String maxResStr = args.optString(0, "1");
        String lang = args.optString(1, Locale.getDefault().toString());
        int maxRes = Integer.parseInt(maxResStr);
        
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); 
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH); //RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"com.zLineup.cordova.plugin.speech"); 
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxRes); 
    } else if(ACTION_STOP.equals(action)) {
        Log.d(TAG,"SpeechRecognition ACTION_STOP ");
        if (!recognizerPresent) {
            fireErrorEvent(ERR_NOT_STARTED);    
            return true;
        }
        loopHandler.post(new Runnable() {
            @Override
            public void run() {
                recognizer.stopListening();
            }
        });
    } else if(ACTION_CANCEL.equals(action)) {
        Log.d(TAG,"SpeechRecognition ACTION_CANCEL ");
        if (!recognizerPresent) {
            fireErrorEvent(ERR_NOT_STARTED);    
            return true;
        }
        loopHandler.post(new Runnable() {
            @Override
            public void run() { 
                recognizer.cancel();
            }
        });
    } else {
        Log.d(TAG,"SpeechRecognition unknown action... ");
        callbackContext.error("SpeechRecognition Unknown action: " + action);
        return false;
    }
    return true;
  }

  private ComponentName pickRecognizer(String prefix){
     List<ResolveInfo> recognizers=myContext.getPackageManager().queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE),0);
     int numRecognizers = recognizers.size();
     ServiceInfo serviceInfo;
     
      if (numRecognizers == 0) {
        Log.e(TAG,"SpeechRecognition recognizer not found!");
        fireErrorEvent(ERR_NO_RECOGNIZER);    
        return null;
     }
     else {
        Log.d(TAG,"SpeechRecognition recognizers found: "+numRecognizers);
        for (int i=0; i < numRecognizers; i++) {
            serviceInfo=recognizers.get(i).serviceInfo;
            Log.d(TAG, "SpeechRecognition avaliable - Pkg: "+serviceInfo.packageName+", Name: "+serviceInfo.name);  
            if (serviceInfo.packageName.startsWith(prefix)) {
                Log.i(TAG, "SpeechRecognition picked recognizer -  Pkg: "+serviceInfo.packageName+", Name: "+serviceInfo.name);  
                return new ComponentName(serviceInfo.packageName,serviceInfo.name);
            }
        }
        Log.w(TAG, "No tested recognizers found");              
        fireErrorEvent(ERR_RECOGNIZER_UNTESTED);    
        return null;
    }
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
        pr.setKeepCallback(true);
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
        pr.setKeepCallback(true); // FIXME - false
        callbackContext.sendPluginResult(pr); 
    }

    class SpeechRecognitionListner implements RecognitionListener          
    {
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "SpeechRecognition onReadyForSpeech");
            fireEvent("ready");
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "SpeechRecognition onBeginningOfSpeech");
            fireEvent("start");
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
            Log.d(TAG, "SpeechRecognition onEndofSpeech");
            fireEvent("end");
        }
        public void onError(int error)
        {
            Log.d(TAG,  "SpeechRecognition onError " +  error);
            fireErrorEvent(error);
        }
        public void onResults(Bundle results)                   
        {
            Log.d(TAG, "SpeechRecognition onResults " + results);
            String str = new String();
            ArrayList<String> transcript = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            if (transcript.size() > 0) {
                Log.d(TAG, "SpeechRecognition fire recognition event");
                fireRecognitionEvent(transcript, confidence);
            } else {
                Log.d(TAG, "SpeechRecognition fire no match event");
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
            Log.d(TAG, "SpeechRecognition onEvent " + eventType);
            //fireEvent("event");
        }
    }
}
