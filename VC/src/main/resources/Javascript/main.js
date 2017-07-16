
var IPAddress = "192.168.0.21:8080";
var userName;
var wsURL;
var webSocketConnection = null;


var localPC = null; // change name?
var SDPOffer;
var SDPAnswer;
var caller;
var gotAnswer = false;


var constraints = {video: true, audio: true};

//var startButton = document.getElementById('startButton');

function startCall() {

    caller = true;

    newConnection();

    navigator.mediaDevices.getUserMedia(constraints).then(function(localMediaStream) {
        var localVideoFeed = document.getElementById('localVideoFeed');
        localVideoFeed.muted = true; // mute this otherwise feedback
        localVideoFeed.srcObject = localMediaStream; // display local video feed
        localMediaStream.getTracks().forEach(function (track) { // add local stream to peer connection
            localPC.addTrack(track, localMediaStream)
        });
    }).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in startCall() getUserMedia()");
        console.log(err);
    });
    
}

function startNegotiating() {
    localPC.createOffer().then(function (offer) {
        return localPC.setLocalDescription(offer);
    }).then(function () {
        SDPOffer = {
            type: "offer",
            user: userName,
            sdp: localPC.localDescription
        };
        alert("sending offer");
        msgServer(SDPOffer);
    }).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in startNegotiating()");
        console.log(err);
    });
}

function start() {
   // startButton.disabled = true; // test if this working



    userName = prompt("Please enter you name: ");

    if (userName === null) {
        location.reload();
    }

    wsURL = "ws://" + IPAddress + "/webSocket/0?name=" + userName;

    startWebSocket();

    startCall();

}

function join() {

    userName = prompt("Please enter your name: ");

    if (userName === null) {
        location.reload();
    }

    var roomID = prompt("Please enter the ID of call to join: ");

    if (roomID === null) {
        location.reload();
    }

    wsURL = "ws://" + IPAddress + "/webSocket/" + roomID + "?name=" + userName;

    startWebSocket();

}

function receivedOffer(offer) {

    if (offer.user === userName) {
        return; // this is your own offer!
    }

    newConnection();

    var remoteDesc = new RTCSessionDescription(offer.sdp);

    localPC.setRemoteDescription(remoteDesc).then(function () {
        return navigator.mediaDevices.getUserMedia(constraints);

    }).then(function (localMediaStream) {
        var localVideoFeed = document.getElementById('localVideoFeed');
        localVideoFeed.muted = true;
        localVideoFeed.srcObject = localMediaStream;
        return localMediaStream.getTracks().forEach(function (track) { // add local stream to peer connection
            localPC.addTrack(track, localMediaStream)
        });
    })
    .then(function () {
        return localPC.createAnswer();
    }).then(function (answer) {
        return localPC.setLocalDescription(answer);
    }).then(function () {
        SDPAnswer = {
            type: "answer",
            sdp: localPC.localDescription
        };
        msgServer(SDPAnswer);
    }).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in receivedOffer()");
        console.log(err);
    });
}

function receivedAnswer(answer) {

    var remoteDesc = new RTCSessionDescription(answer.sdp);

    localPC.setRemoteDescription(remoteDesc).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in receivedAnswer()");
        console.log(err);
    });

    gotAnswer = true;
}

function startWebSocket() {

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
            var msg = {
                type: "chat-message",
                user: userName,
                msg: userName + ": " + $chatMessage.val()
            };
            $chatMessage.val("");
            msgServer(msg);
        })
    };

    webSocketConnection.onerror = function (error) {
        console.log("WebSocket error " + error);
        console.log(error);
    };

    webSocketConnection.onmessage = function (evt) {
        if (evt.data.substring(0, 7) === "Welcome" || evt.data.substring(0, 8) === "The Room") {

            alert(evt.data); // Welcome to Room || Room doesn't exist
            if (evt.data === "The Room ID entered does not exist.") {
                webSocketConnection.close();
                location.reload();
            }

        } else if (evt.data.substring(0, 4) === "user") {

            var user = evt.data.substring(4, evt.data.length); // adding user to attendee list
            $attendees.html("Attendees:");
            $attendeeList.append($("<li>" + user + "</li>"));

            msgServer(SDPOffer); // new user has joined so send them the offer

        } else if (evt.data.substring(0, 2) === "ID") {

            $roomNumber.html("You are in Room " + evt.data); // room number

        } else {


            var msg = JSON.parse(evt.data);


            switch(msg.type) {

                case "offer":
                    alert("Received offer");
                    receivedOffer(msg);
                    break;

                case "answer":
                    alert("received answer");
                    receivedAnswer(msg);
                    break;

                case "ice-candidate":
                    newICECandidateMessage(msg);
                    break;

                case "chat-message":
                    $messages.prepend($("<li>" + msg.msg + "</li>"));
                    break;
            }
        }
    };

    webSocketConnection.onclose = function() {
        alert("WebSocket Closed");
    };
}

function msgServer (message) {

    try {
        var jsonMsg = JSON.stringify(message);
    } catch (err) {
        alert(err.name + ": " + err.message)
    }

    webSocketConnection.send(jsonMsg);
}

function newConnection() {

    localPC = new RTCPeerConnection();

    localPC.onicecandidate = handleICECandidateEvent;
    localPC.ontrack = handleTrackEvent;
    localPC.onnegotiationneeded = startNegotiating;

}

function handleICECandidateEvent(event) {

    if (event.candidate) {
        var ICECandidate = {
            type: "ice-candidate",
            user: userName,
            candidate: event.candidate
        };
        msgServer(ICECandidate);
    }
}

function newICECandidateMessage(msg) {

    if (caller && !gotAnswer) {
        return;
    }
    if (msg.user === userName) {
        return; // own message!
    }

    var candidate = msg.candidate;

    localPC.addIceCandidate(candidate).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in newICECandidateMessage()");
        console.log(err);
    });
}

function handleTrackEvent(event) {

    var remoteVideoFeed = document.getElementById('remoteVideoFeed');
    remoteVideoFeed.srcObject = event.streams[0];
}




