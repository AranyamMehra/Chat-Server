package Protocol

object MessageProtocol {
    def ecnode(message: Message): String = message match {
        case Connect(username: String) => s"CONNECT|$username"
        case DisconnectMessage() => "DISCONNECT"
        case Broadcast(from: String, text: String) => s"BROADCAST|$from|$text"
        case Private(from: String, to: String, text: String) => s"PRIVATE|$from|$to|$text"

        case Connected() => s"CONNECTED"
        case BroadcastDelivered(from: String, text: String) => s"BROADCAST_DELIVERED|$from|$text"
        case PrivateDelivered(from: String, to: String, text: String) => s"PRIVATE_DELIVERED|$from|$to|$text"
        case WelcomeMessage(success: Boolean, message: String) => s"WELCOME|$success|$message"

        case UserListMessage(users: List[String]) =>
            if (users.isEmpty) "USERLIST|"
            else s"USERLIST|${users.mkString(",")}"
        case UserJoined(username: String) => s"USER_JOINED|$username"
        case UserLeft(username:String) => s"USER_LEFT|$username"
        case ServerShutdown() => "SERVER_SHUTDOWN"
    }

    def decode(str: String): Option[Message] = {
        val parts = str.split("\\|", -1)
        if (parts.isEmpty) return None

        parts(0) match {
            case "CONNECT" if parts.length >= 2 => Some(Connect(parts(1)))
            case "BROADCAST" if parts.length >= 3 => Some(Broadcast(parts(1), parts(2)))
            case "PRIVATE" if parts.length >= 4 => Some(Private(parts(1), parts(2), parts(3)))
            case "DISCONNECT" => Some(DisconnectMessage())

            case "WELCOME" if parts.length >= 3 => Some(WelcomeMessage(parts(1).toBoolean, parts(2)))
            case "USERLIST" =>
                if (parts.length < 2 || parts(1).isEmpty) {
                    Some(UserListMessage(List()))
                } else {
                    Some(UserListMessage(parts(1).split(",").toList))
                }

            case "BROADCAST_DELIVERED" if parts.length >= 3 => Some(BroadcastDelivered(parts(1), parts(2)))
            case "PRIVATE_DELIVERED" if parts.length >= 4 => Some(PrivateDelivered(parts(1), parts(2), parts(3)))
            case "USER_JOINED" if parts.length >= 2 => Some(UserJoined(parts(1)))
            case "USER_LEFT" if parts.length >= 2 => Some(UserLeft(parts(1)))
            case "SERVER_SHUTDOWN" => Some(ServerShutdown())
            case _ => None

        }
    }
}
