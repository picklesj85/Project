
function start() {
    startButton.disabled = true;
    navigator.mediaDevices.getUserMedia({
        audio: false,
        video: true
    }).then(function(mediaStream) {
        var video = document.getElementById('localVideoFeed');
        video.srcObject = mediaStream;
        //video.play();
    }).catch(function (err) {
        alert(err.name + ": " + err.message);
        })
}

function webSocketTest(msg) {

    var ws = new WebSocket("ws://192.168.0.13:8080/webSocket/1");

    ws.onopen = function () {
        //alert("WebSocket is open");
        ws.send(msg.value);
    };

    ws.onmessage = function (evt) {
        alert(evt.data);
    };

    ws.onclose = function () {
        alert("WebSocket is now closed.");
    };

}