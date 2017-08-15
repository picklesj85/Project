package server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


trait MyJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val sendUserFormat = jsonFormat2(SendUser)
  implicit val roomIDFormat = jsonFormat3(RoomID)
  implicit val roomErrorFormat = jsonFormat1(RoomError)
  implicit val wrappedMessageFormat = jsonFormat1(WrappedMessage)
  implicit val roomNumber = jsonFormat2(RoomNumber)
  implicit val onlineContacts = jsonFormat2(OnlineContacts)
  implicit val offlineContacts = jsonFormat2(OfflineContacts)
  implicit val pendingContacts = jsonFormat2(PendingContacts)
  implicit val receiveCall = jsonFormat3(ReceiveCall)
  implicit val sendCall = jsonFormat2(SendCall)
  implicit val accepted = jsonFormat1(Accepted)
  implicit val rejected = jsonFormat1(Rejected)
  implicit val results = jsonFormat2(Results)
}
