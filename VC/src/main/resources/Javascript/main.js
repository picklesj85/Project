"use strict";

var IPAddress = "192.168.0.21:8080";
var userName;
var roomID;
var wsURL;
var webSocketConnection = null;


var localPC = null; // change name?
var SDPOffer;
var SDPAnswer = "not yet defined";
var caller;
var gotAnswer = false;
var otherUserPresent = false; // redundant?
var callEnded = false;


var constraints = {video: true, audio: true};

//var startButton = document.getElementById('startButton');

function start() {
    var ready = {
        tag: "ready",
        user: userName
    };
    msgServer(ready);
}



function getMedia() {

    localPC = new RTCPeerConnection();

    navigator.mediaDevices.getUserMedia(constraints).then(function(localMediaStream) {
        var localVideoFeed = document.getElementById('localVideoFeed');
        localVideoFeed.muted = true; // mute this otherwise feedback
        localVideoFeed.srcObject = localMediaStream; // display local video feed
        localMediaStream.getTracks().forEach(function (track) { // add local stream to peer connection
            localPC.addTrack(track, localMediaStream)
        });
        localVideoFeed.onloadedmetadata = function () {
            prepareWebSocket();
        }
    }).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in startCall() getUserMedia()");
        console.log(err);
    });
    
    //prepareWebSocket();
}

function prepareWebSocket() {

    let params = (new URL(document.location)).searchParams;
    userName = params.get("userName");
    roomID = params.get("roomID");

    console.log("User: " + userName + "\nRoom: " + roomID);

    wsURL = "ws://" + IPAddress + "/webSocket/" + roomID + "?name=" + userName;

    newConnection();

    startWebSocket();

}

function startNegotiating() {
    localPC.createOffer().then(function (offer) {
        return localPC.setLocalDescription(offer);
    }).then(function () {
        SDPOffer = {
            tag: "offer",
            user: userName,
            sdp: localPC.localDescription
        };
        console.log("Sending Offer");
        msgServer(SDPOffer);
    }).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in startNegotiating()");
        console.log(err);
    });

    localPC.onnegotiationneeded = startNegotiating;
    
    // setTimeout(function () {
    //     if (!gotAnswer) {
    //         localPC.close();
    //         webSocketConnection.close();
    //         getMedia();
    //     }
    // }, 3000);
}

function receivedOffer(offer) {

    if (offer.user === userName) {
        return; // this is your own offer!
    }

    console.log("Received offer");

    newConnection();

    var remoteDesc = new RTCSessionDescription(offer.sdp);

    localPC.setRemoteDescription(remoteDesc).then(function () {
        return localPC.createAnswer();
    }).then(function (answer) {
        return localPC.setLocalDescription(answer);
    }).then(function () {
        SDPAnswer = {
            tag: "answer",
            user: userName,
            sdp: localPC.localDescription
        };
        console.log("Sending Answer");
        msgServer(SDPAnswer);
    }).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in receivedOffer()");
    });

}


function receivedAnswer(answer) {

    if (answer.user === userName) {
        return; // own answer
    }

    console.log("Received Answer");
    //alert("received answer");

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
        $room = $("#room"),
        $attendees = $("#attendees"),
        $attendeeList = $("#attendeeList"),
        $title = $("#title");


    webSocketConnection = new WebSocket(wsURL);

    webSocketConnection.onopen = function () {
        var userJson = {
            tag: "user",
            userName: userName
        };
        msgServer(userJson);
        $send.on('click', function () {
            var msg = {
                tag: "chat-message",
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

        console.log("From server: " + evt.data);

        var msg = JSON.parse(evt.data);

        switch(msg.tag) {

            case "roomError":
                alert("The Room ID entered does not exist.");
                webSocketConnection.close();
                window.location.href = "/";
                break;

            case "user":
                if (msg.userName !== userName) {
                    otherUserPresent = true; //redundant?
                    if (caller) {
                        startNegotiating();
                    }
                }
                $attendees.html("Attendees:");
                $attendeeList.append($("<li>" + msg.userName + "</li>"));
                break;

            case "roomID":
                $room.html("You are in Room " + msg.roomID.toString());
                $title.append(" " + msg.roomID.toString());
                break;

            case "ready":


            case "offer":
                receivedOffer(msg);
                break;

            case "answer":
                receivedAnswer(msg);
                break;

            case "ice-candidate":
                newICECandidateMessage(msg);
                break;

            case "chat-message":
                $messages.prepend($("<li>" + msg.msg + "</li>"));
                break;

            case "hangUp":
                webSocketConnection.close();
                alert("Call has been ended.");
                window.location.href = "/";
                break;

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

    console.log("To server: " + jsonMsg);

    try {
        webSocketConnection.send(jsonMsg);
    } catch (error) {
        console.log(error);
    }
}

function newConnection() {

    localPC.onicecandidate = handleICECandidateEvent;
    localPC.ontrack = handleTrackEvent;

}

function handleICECandidateEvent(event) {

    if (event.candidate) {
        var ICECandidate = {
            tag: "ice-candidate",
            user: userName,
            candidate: event.candidate
        };
        msgServer(ICECandidate);
    }
}

function newICECandidateMessage(msg) {

    // if (caller && !gotAnswer) {
    //     return;
    // }
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

function endCall() {

    callEnded = true;

    var hangUp = {
        tag: "hangUp"
    };
    msgServer(hangUp);
}



