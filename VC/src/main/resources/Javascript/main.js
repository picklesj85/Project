
var webSocketConnection;
var userName;
var wsURL;
var IPaddress = "192.168.0.21:8080";

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

    wsURL = "ws://" + IPaddress + "/webSocket/0?name=" + userName;

    handleWebSocket();

}

function join() {
    var roomID = prompt("Please enter the ID of call to join: ");

    userName = prompt("Please enter your name: ");

    wsURL = "ws://" + IPaddress + "/webSocket/" + roomID + "?name=" + userName;

    handleWebSocket();

}

function handleWebSocket() {

    var $chatMessage = $("#chatMessage"),
        $send = $("#send"),
        $messages = $("#messages"),
        $roomNumber = $("#roomNumber"),
        $attendees = $("#attendees"),
        $attendeeList = $("#attendeeList");


    webSocketConnection = new WebSocket(wsURL);

    webSocketConnection.onopen = function () {
        webSocketConnection.send("user" + userName);
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
        if (isNaN(evt.data) && evt.data.charAt(0) != '*' && evt.data.substring(0, 4) != "user") {
            alert(evt.data);
            if (evt.data === "The Room ID entered does not exist.") {
                webSocketConnection.close();
                location.reload();
            }
        } else if (evt.data.charAt(0) === '*'){
            var msg = evt.data.substring(1, evt.data.length);
            $messages.prepend($("<li>" + msg + "</li>"));
        } else if (evt.data.substring(0, 4) === "user") {
            var user = evt.data.substring(4, evt.data.length);
            $attendees.html("Attendees:");
            $attendeeList.append($("<li>" + user + "</li>"));
        } else {
            $roomNumber.html("You are in Room " + evt.data);
            //document.getElementById('roomNumber').innerHTML = "You are in room " + evt.data
        }
        //return false;
    };

    webSocketConnection.onclose = function() {
        alert("WebSocket Closed");
    }
}
