package Protocol
trait Message extends Serializable

case class Connect (username: String) extends Message
case class Broadcast (from: String, text: String) extends Message
case class Private (from: String,to: String, text: String) extends Message
case class DisconnectMessage() extends Message

case class Connected() extends Message
case class BroadcastDelivered(from: String, text: String) extends Message
case class PrivateDelivered(from: String, to: String, text: String) extends Message
case class WelcomeMessage(success: Boolean, message: String) extends Message
case class UserListMessage(users: List[String]) extends Message
case class UserJoined(username: String) extends Message
case class UserLeft(username: String) extends Message
case class ServerShutdown() extends Message