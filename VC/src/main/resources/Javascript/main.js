
var webSocketConnection;
var userName;
var wsURL;

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

    userName = prompt("Please enter you name: ");

    wsURL = "ws://192.168.0.13:8080/webSocket/0";

    handleWebSocket();

}

function join() {
    var roomID = prompt("Please enter the ID of call to join: ");

    userName = prompt("Please enter your name: ");

    wsURL = "ws://192.168.0.13:8080/webSocket/" + roomID;

    handleWebSocket();

}

function handleWebSocket() {

    var $chatMessage = $("#chatMessage"),
        $send = $("#send"),
        $messages = $("#messages"),
        $roomNumber = $("#roomNumber");


    webSocketConnection = new WebSocket(wsURL);

    webSocketConnection.onopen = function () {
        $send.on('click', function () {
            var msg = "*" + userName + ": " + $chatMessage.val();
            $chatMessage.val("");
            webSocketConnection.send(msg);
        })
    };

    webSocketConnection.onerror = function (error) {
        console.log("WebSocket error " + error);
    };

    webSocketConnection.onmessage = function (evt) {
        if (isNaN(evt.data) && evt.data.charAt(0) != '*') {
            alert(evt.data);
            if (evt.data === "The Room ID entered does not exist.") {
                webSocketConnection.close();
                location.reload();
            }
        } else if (evt.data.charAt(0) === '*'){
            var msg = evt.data.substring(1, evt.data.length);
            $messages.prepend($("<li>" + msg + "</li>"));
        } else {
            $roomNumber.html("You are in room " + evt.data);
            //document.getElementById('roomNumber').innerHTML = "You are in room " + evt.data
        }
        //return false;
    };

    webSocketConnection.onclose = function() {
        alert("WebSocket Closed");
    }
}
