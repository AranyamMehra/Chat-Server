package Protocol

object MessageProtocol {
    def ecnode(message: Message): String = message match {
        case Connect(username: String) => s"CONNECT|$username"
        case DisconnectMessage() => "DISCONNECT"
        case Broadcast(from: String, text: String) => s"BROADCAST|$text"
        case Private(from: String, to: String, text: String) => s"PRIVATE|$to|$text"

        case Connected() => s"CONNECTED"
        case BroadcastDelivered(from: String, text: String) => s"BROADCAST_DELIVERED|$from|$text"
        case PrivateDelivered(from: String, to: String, text: String) => s"PRIVATE_DELIVERED|$from|$to|$text"
        case WelcomeMessage(success: Boolean, message: String) => s"WELCOME|$success|$message"
        case UserListMessage(users: List[String]) => s"USERLIST|${users.mkString(",")}"
        case UserJoined(username: String) => s"USER_JOINED|$username"
        case UserLeft(username: String) => s"USER_LEFT|$username"
    }

    def decode(str: String): Option[Message] = {
        val parts = str.split("\\|")
        parts(0) match {
            case "CONNECT" => Some(Connect(parts(1)))
            case "BROADCAST" => Some(Broadcast(parts(1), parts(2)))
            case "PRIVATE" => Some(Private(parts(1), parts(2), parts(3)))
            case "DISCONNECT" => Some(DisconnectMessage())

            case "WELCOME" => Some(WelcomeMessage(parts(1).toBoolean, parts(2)))
            case "USERLIST" =>
                val users = if (parts(1).isEmpty) List() else parts(1).split(",").toList
                Some(UserListMessage(users))
            case "BROADCAST_RCV" => Some(BroadcastDelivered(parts(1), parts(2)))
            case "PRIVATE_RCV" => Some(PrivateDelivered(parts(1), parts(2), parts(3)))
            case "USER_JOINED" => Some(UserJoined(parts(1)))
            case "USER_LEFT" => Some(UserLeft(parts(1)))
            case _ => None

        }
    }
}
