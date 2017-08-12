
var user;
var serverAddress = "192.168.0.21:8080";
var webSocket;

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

            case "onlineUsers":
                var onlineUsers = msg.onlineUsers; // this is the list of usernames from server that are online
                var userList = "";
                for (var i = 0; i < onlineUsers.length; i++) {
                    if (onlineUsers[i] !== user) { // do not want to display our own name
                        userList += "<li onclick='call(this.innerHTML)'>" + onlineUsers[i] + "</li>";
                    }
                }
                document.getElementById("onlineUsers").innerHTML = userList; // display the list
                break;

            case "roomNumber":
                alert("Please use room number: " + msg.number +
                    "\n\nMake sure you share this number with the far site.");
                break;
        }
    };

    webSocket.onclose = function() {
        console.log("WebSocket Closed.")
    };

}

function call(onlineUser) {
    msgServer("call" + onlineUser);
}

function createRoom() {
    msgServer("createRoom");
}

function logout() {
    msgServer("logout");
    window.location.href = "/";
}
function msgServer (message) {

    try {
        webSocket.send(message);
    } catch (error) {
        console.log(error);
    }

    console.log("To server: " + message);
}