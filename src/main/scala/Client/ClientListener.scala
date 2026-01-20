package Client

import Protocol._

class ClientListener (connection: ClientConnection) {
    def listen(): Unit = {
        new Thread(() => {
            var running = true
            while (running) {
                val raw = connection.in.readLine()
                if (raw == null) {
                    println("Disconnected from server")
                    running = false
                } else {
                    handle(raw)
                }
            }
        }).start()
    }

    private def handle(raw: String): Unit = {
        MessageProtocol.decode(raw) match {
            case Some(BroadcastDelivered(from, text)) =>
                println(s"[$from]: $text")

            case Some(PrivateDelivered(from, _, text)) =>
                println(s"[Private from $from]: $text")

            case Some(UserListMessage(users)) =>
                println("Connected users:")
                users.foreach(u => println(s" - $u"))

            case Some(UserJoined(user)) =>
                println(s"$user joined")

            case Some(UserLeft(user)) =>
                println(s"$user left")

            case Some(WelcomeMessage(_, msg)) =>
                println(msg)

            case _ =>
                println(s"Unknown: $raw")
        }
    }
}