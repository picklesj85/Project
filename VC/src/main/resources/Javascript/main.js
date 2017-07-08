
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

function webSocketTest() {

    var ws = new WebSocket("ws://192.168.0.3:8080/webSocketTest");

    ws.onopen = function () {
        //alert("WebSocket is open");
        ws.send("James");
    };

    ws.onmessage = function (evt) {
        alert(evt.data);
    };

    ws.onclose = function () {
        alert("WebSocket is now closed.");
    };

}