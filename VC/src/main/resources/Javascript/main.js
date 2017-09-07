"use strict";

var IPAddress = "172.20.10.3:8080";
var userName;
var roomID;
var wsURL;
var webSocketConnection = null;


var localPC = null; // change name?
var SDPOffer = null;
var SDPAnswer = null;
var caller;
var callEnded = false;
var myMediaStream;
var offerFailed = true;
var answerFailed = true;
var gotOffer = false;
var gotAnswer = false;

var attendees = [];

var constraints = {video: true, audio: true};


function getMedia() {

    navigator.mediaDevices.getUserMedia(constraints).then(function(localMediaStream) {
        var localVideoFeed = document.getElementById('localVideoFeed');
        localVideoFeed.muted = true; // mute this otherwise feedback
        localVideoFeed.srcObject = localMediaStream; // display local video feed
        myMediaStream = localMediaStream; // assign to global for reconnection if failure
        
        localVideoFeed.onloadedmetadata = function () {
            prepareWebSocket();
        }
    }).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in startCall() getUserMedia()");
        console.log(err);
    });

}

function prepareWebSocket() {

    let params = (new URL(document.location)).searchParams;
    userName = params.get("userName");
    roomID = params.get("roomID");
    document.getElementById("localUsername").innerHTML = userName;

    console.log("User: " + userName + "\nRoom: " + roomID);

    wsURL = "ws://" + IPAddress + "/webSocket/" + roomID + "?name=" + userName;

    newPeerConnection();

    startWebSocket();

}

function startNegotiating() {
    if (localPC.signalingState === "have-local-offer") {
        return; // already have local offer
    }

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
    
    setTimeout(function () { // timeout function for when sending the offer has failed
        if (offerFailed) {
            localPC.close();
            webSocketConnection.close();
            newPeerConnection();
            startWebSocket();
        }
    }, 500);
}

function receivedOffer(offer) {

    if (gotOffer) {
        return;
    }

    if (offer.user === userName) {
        offerFailed = false; // the server has relayed my offer so I know my connection is good

        return; // this is your own offer!
    }

    gotOffer = true;

    console.log("Received offer");

    newPeerConnection();

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

    setTimeout(function () { // timeout function for when sending the answer has failed
        if (answerFailed) {
            localPC.close();
            webSocketConnection.close();
            gotOffer = false;
            newPeerConnection();
         //   prepareWebSocket();
            startWebSocket();
        }
    }, 500);
}

function receivedAnswer(answer) {

    if (gotAnswer) {
        return;
    }

    if (answer.user === userName) {
        answerFailed = false; // the server has relayed my offer so I know my connection is good
        return; // own answer
    }

    gotAnswer = true;

    if (localPC.signalingState === "stable") {
        return; // already connected
    }
    console.log("Received Answer");
    //alert("received answer");

    var remoteDesc = new RTCSessionDescription(answer.sdp);

    localPC.setRemoteDescription(remoteDesc).catch(function (err) {
        alert(err.name + ": " + err.message + "\n\n in receivedAnswer()");
        console.log(err);
    });

}

function startWebSocket() {

    var $chatMessage = $("#chatMessage"),
        $send = $("#send"),
        $messages = $("#messages"),
        $room = $("#room"),
        $loading = $("#loading"),
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
        console.log("WebSocket error " + error.toString());
        console.log(error);
        // if (caller && offerFailed)
        // webSocketConnection.close();
        // offerFailed = true;
        // answerFailed = true;
        // prepareWebSocket();
    };

    webSocketConnection.onmessage = function (evt) {

        console.log("From server: " + evt.data);

        var msg = JSON.parse(evt.data);

        switch(msg.tag) {

            case "roomError":
                alert("The Room ID entered does not exist.");
                webSocketConnection.close();
                redirect();
                break;

            case "user":
                if (msg.userName !== userName) {
                    if (caller && offerFailed) {
                        // if (!offerFailed) { // the other side has a connection issue so need to start over.
                        //     localPC.close();
                        //     newPeerConnection();
                        // }
                        startNegotiating();
                    }
                }
                if (msg.userName !== userName) {
                    document.getElementById("remoteUsername").innerHTML = msg.userName;
                }
                break;

            case "roomID":
                if(msg.caller) {
                    caller = true;
                }
                $room.html("You are in Room " + msg.roomID.toString());
                $title.append(" " + msg.roomID.toString());
                break;

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
                redirect();
        }

    };

    webSocketConnection.onclose = function() {
        console.log("WebSocket Closed.")
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

function newPeerConnection() {

    localPC = new RTCPeerConnection();

    myMediaStream.getTracks().forEach(function (track) { // add local stream to peer connection
        localPC.addTrack(track, myMediaStream)
    });

    localPC.onicecandidate = handleICECandidateEvent;
    localPC.ontrack = handleTrackEvent;
    // localPC.oniceconnectionstatechange = function () {
    //     if (localPC.iceConnectionState === "failed" ||
    //         localPC.iceConnectionState === "closed") {
    //         iceFailed();
    //     }
    // }
    localPC.onsignalingstatechange = function () {
        if ((localPC.signalingState === "stable" && !offerFailed) ||
            (localPC.signalingState === "stable" && !answerFailed)) {
            webSocketConnection.send("connected");
            console.log("connected");

        }
    }
}

function iceFailed() {
    localPC.close();
    webSocketConnection.close();
    offerFailed = true;
    answerFailed = true;
   // newPeerConnection();
    prepareWebSocket();
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

    if (msg.user === userName || (caller && !gotAnswer) || (!caller && !gotOffer)) {
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
    remoteVideoFeed.removeAttribute("class");
    $("#loadText").remove();
    remoteVideoFeed.srcObject = event.streams[0];
}

function endCall() {

    callEnded = true;

    var hangUp = {
        tag: "hangUp"
    };
    msgServer(hangUp);
    redirect();
}

function redirect() {

    // takes you back to your user homepage if you are logged in, if not back to the site homepage

    $.get("/home?user=" + userName, function (data) {
        if (data.toString() === "You must be logged in to view this page.") {
            window.location.href = "/";
        } else {
            window.location.href = "/home?user=" + userName;
        }
    });

}




