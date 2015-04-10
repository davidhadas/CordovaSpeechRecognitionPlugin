module.exports = function () {
    
    this.init = function(success, error, maxMatches, language) {
        //console.log('SpeechRecognitionPlugin: init');
        this.success = success;
        this.error = error;
        return cordova.exec(success, error, "SpeechRecognition", "init", [maxMatches, language]);
    };

    this.start = function() {
        //console.log('SpeechRecognitionPlugin: start');
        return cordova.exec(this.success, this.error, "SpeechRecognition", "start", []);
    };

    this.stop = function() {
        //console.log('SpeechRecognitionPlugin: stop');
        return cordova.exec(this.success, this.error, "SpeechRecognition", "stop", []);
    };
}

