function loadFunction() {
    let params = (new URL(document.location)).searchParams;
    var user = params.get("user");

    document.getElementById("title").innerHTML = user + " | Instant VC";
    document.getElementById("user").innerHTML = user;

}

function createRoom() {
    
}
