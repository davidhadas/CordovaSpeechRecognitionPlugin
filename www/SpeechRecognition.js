module.exports = {
    
    init: function(success, error) {
        this.success = success;
        this.error = error;
        
        console.log('SpeechRecognitionPlugin: init');
        cordova.exec(success, error, "SpeechRecognition", "init", []);
    },

    config: function(maxMatches, language) {
        console.log('SpeechRecognitionPlugin: config');
        cordova.exec(this.success, this.error, "SpeechRecognition", "config", [maxMatches, language]);
    },

    start: function() {
        console.log('SpeechRecognitionPlugin: start');
        cordova.exec(this.success, this.error, "SpeechRecognition", "start", []);
    },

    stop: function() {
        console.log('SpeechRecognitionPlugin: stop');
        cordova.exec(this.success, this.error, "SpeechRecognition", "stop", []);
    },
        
    cancel: function() {
        console.log('SpeechRecognitionPlugin: cancel');
        cordova.exec(this.success, this.error, "SpeechRecognition", "cancel", []);
    }
}

