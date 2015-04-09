
function SpeechRecognition() {
    this.init = function(success, error, maxMatches, language) {
        return cordova.exec(success, error, "SpeechRecognition", "init", [maxMatches, language]);
    };

    this.start = function(success, error) {
        return cordova.exec(success, error, "SpeechRecognition", "start");
    };

    this.stop = function(success, error, maxMatches, language) {
        return cordova.exec(success, error, "SpeechRecognition", "stop");
    };
}
