
var user;
var serverAddress = "192.168.0.21:8080";
var webSocket;
var room;

function loadFunction() {
    let params = (new URL(document.location)).searchParams;
    user = params.get("user");

    document.getElementById("title").innerHTML = user + " | Instant VC";
    document.getElementById("user").innerHTML = user;
    document.getElementById("userName").value = user;

    startWebSocket();
}

function startWebSocket() {

    var wsURL = "ws://" + serverAddress + "/loggedIn?user=" + user;

    webSocket = new WebSocket(wsURL);

    webSocket.onerror = function (error) {
        console.log("WebSocket error: " + error);
    };

    webSocket.onmessage = function (evt) {

        console.log("From server: " + evt.data);

        var msg = JSON.parse(evt.data);

        switch(msg.tag) {

            case "online":
                var online = msg.onlineContacts; // this is the list of contacts from server that are online
                var onlineList = "";
                for (var i = 0; i < online.length; i++) {
                    if (online[i] !== user) { // do not want to display our own name
                        onlineList += "<li onclick='call(this.innerHTML)'>" + online[i] + "</li>";
                    }
                }
                document.getElementById("online").innerHTML = onlineList; // display the list
                break;

            case "offline":
                var offline = msg.offlineContacts; // this is the list of contacts from server that are offline
                var offlineList = "";
                for (i = 0; i < offline.length; i++) {
                    if (offline[i] !== user) { // do not want to display our own name
                        offlineList += "<li>" + offline[i] + "</li>";
                    }
                }
                document.getElementById("offline").innerHTML = offlineList; // display the list
                break;

            case "pending":
                var pending = msg.pendingContacts; // this is the list of contact requests
                var pendingList = "";
                for ( i = 0; i < pending.length; i++) {
                    if (pending[i] !== user) { // do not want to display our own name
                        pendingList += "<li onclick='respond(this.innerHTML)'>" + pending[i] + "</li>";
                    }
                }
                document.getElementById("pending").innerHTML = pendingList; // display the list
                break;

            case "roomNumber":
                alert("Please use room number: " + msg.number +
                    "\n\nMake sure you share this number with the far site.");
                break;

            case "receiveCall":
                var caller = msg.user;
                room = msg.room;
                if (confirm("Incoming call from " + caller + ". \n\nAccept call?")) {
                    msgServer("accepted" + caller);
                    msgServer("onCall");
                    window.location.href = "/room.html?roomID=" + room + "&userName=" + user;
                } else {
                    msgServer("rejected" + caller);
                }
                break;

            case "sendCall":
                room = msg.room;
                break;

            case "accepted":
                msgServer("onCall");
                window.location.href = "/room.html?roomID=" + room + "&userName=" + user;
                break;

            case "rejected":
                alert("The far end has rejected your call.");
                window.location.reload();

            // case onlineContacts
            // case offlineContacts
            // case pendingContacts
            // case searchContacts
        }
    };

    webSocket.onclose = function() {
        console.log("WebSocket Closed.");
        startWebSocket();
    };

}

function respond(contact) {
    if (confirm("Accept contact request from " + contact + "? \n\nClick OK to confirm or Cancel to reject.")) {
        msgServer("respondaccept" + contact);
    } else {
        msgServer("respondreject" + contact);
    }
}

function searchContacts(){

}

function call(onlineUser) {
    document.getElementById("calling").setAttribute("class", "calling");
    document.getElementById("callTxt").innerHTML = "Calling...";
    msgServer("call" + onlineUser);
}

function createRoom() {
    msgServer("createRoom");
}

function logout() {
    msgServer("logout");
    window.location.href = "/";
}

function onCall() {
    msgServer("onCall");
}

function msgServer (message) {

    try {
        webSocket.send(message);
    } catch (error) {
        console.log(error);
    }

    console.log("To server: " + message);
}