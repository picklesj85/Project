
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