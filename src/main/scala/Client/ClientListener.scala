package Client

import Protocol._

class ClientListener (connection: ClientConnection) {
    var listenerThread: Thread = _
    @volatile var running = true

    def listen(): Unit = {
        listenerThread = new Thread(() => {
            var running = true
            while (running) {
                try {
                    val raw = connection.in.readLine()
                    if (raw == null) {
                        println("[SYSTEM] Disconnected from server")
                        running = false
                    }
                    else {
                        handle(raw)
                    }
                } catch {
                    case e: java.net.SocketException if !running =>
                        println("[SYSTEM] Connection closed")
                    case e: Exception =>
                        if (running) {
                            println(s"[ERROR] Listener error: ${e.getMessage}")
                        }
                        running = false
                }
            }
            println("[LISTENER] Thread stopped")
        })
        listenerThread.start()
    }

    def stop(): Unit = {
        running = false
        if (listenerThread != null && listenerThread.isAlive) {
            listenerThread.interrupt()
            try {
                listenerThread.join(2000)
            }
            catch {
                case _: InterruptedException =>
            }
        }
    }


    private def handle(raw: String): Unit = {
        MessageProtocol.decode(raw) match {
            case Some(BroadcastDelivered(from, text)) =>
                println(s"[BROADCAST FROM $from]: $text")

            case Some(PrivateDelivered(from, _, text)) =>
                println(s"[Private from $from]: $text")

            case Some(UserListMessage(users)) =>
                if (users.nonEmpty) {
                    println("[ONLINE USERS]")
                    users.foreach(u => println(s"  - $u"))
                }
                else {
                    println("[ONLINE USERS] No other users online")
                }

            case Some(UserJoined(user)) =>
                println(s"[SYSTEM] $user joined the chat")

            case Some(UserLeft(user)) =>
                println(s"[SYSTEM] $user left the chat")

            case Some(WelcomeMessage(success, msg)) =>
                if (success) {
                    println(s"[SERVER] $msg")
                }
                else {
                    println(s"[SERVER] ERROR: $msg")
                }

            case None =>
                println(s"Could not decode: $raw")

            case _ =>
                println(s"Unknown: $raw")
        }
    }
}