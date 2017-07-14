
var webSocketConnection;

function start() {
    // startButton.disabled = true;
    // navigator.mediaDevices.getUserMedia({
    //     audio: false,
    //     video: true
    // }).then(function(mediaStream) {
    //     var video = document.getElementById('localVideoFeed');
    //     video.srcObject = mediaStream;
    //     //video.play();
    // }).catch(function (err) {
    //     alert(err.name + ": " + err.message);
    //     })

    webSocketConnection = new WebSocket("ws://192.168.0.13:8080/webSocket/0");

    webSocketConnection.onopen = function() {
        alert("WebSocket Open");
        //webSocketConnection.send("testing....");
    };

    webSocketConnection.onmessage = function (evt) {
        if (isNaN(evt.data)) {
            alert(evt.data);
        } else {
            document.getElementById('roomNumber').innerHTML = "You are in room " + evt.data;
        }
        return false;
    };

    //webSocketConnection.onclose = alert("WebSocket Closed");
}

function join() {
    var roomID = prompt("Please enter the ID of call to join: ");

    var wsURL = "ws://192.168.0.13:8080/webSocket/" + roomID;

    webSocketConnection = new WebSocket(wsURL);

    webSocketConnection.onopen = function () {
        alert("WebSocket Open");
    };

    webSocketConnection.onmessage = function (evt) {
        if (isNaN(evt.data)) {
            alert(evt.data);
        } else {
            document.getElementById('roomNumber').innerHTML = "You are in room " + evt.data;
        }
        return false;
    };

    //webSocketConnection.onclose = alert("WebSocket Closed");
}

function webSocketTest(msg) {

   // var ws = new WebSocket("ws://192.168.0.13:8080/webSocket/1");

    // ws.onopen = function () {
    //     //alert("WebSocket is open");
    //     ws.send(msg.value);
    // };

    webSocketConnection.send(msg.value);

    webSocketConnection.onmessage = function (evt) {
        //alert(evt.data);
        document.getElementById('sent').innerHTML = evt.data;
        return false;
    };

    webSocketConnection.onclose = function () {
        alert("WebSocket is now closed.");
    };

}