
function SpeechRecognition() {
    this.init = function(success, error) {
        return cordova.exec(success, error, "SpeechRecognition", "init");
    };

    this.start = function(success, error, maxMatches, language) {
        return cordova.exec(success, error, "SpeechRecognition", "start", [maxMatches, language]);
    };

    this.stop = function(success, error, maxMatches, language) {
        return cordova.exec(success, error, "SpeechRecognition", "stop");
    };
}
