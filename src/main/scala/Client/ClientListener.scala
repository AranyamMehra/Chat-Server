package Client

import Protocol._
import Utility.Logger

class ClientListener (connection: ClientConnection, username: String) {
    private val logger = Logger(s"ClientListener-$username")
    var listenerThread: Thread = _
    @volatile var running = true

    def listen(): Unit = {
        logger.info("Starting message listener thread")

        listenerThread = new Thread(() => {
            var running = true
            while (running) {
                try {
                    val raw = connection.in.readLine()
                    if (raw == null) {
                        logger.warn("Server closed connection")
                        println("[SYSTEM] Disconnected from server")
                        running = false
                    }
                    else {
                        logger.debug(s"Received: $raw")
                        handle(raw)
                    }
                } catch {
                    case e: java.net.SocketException if !running =>
                        logger.info("Socket closed (expected)")
                        println("[SYSTEM] Connection closed")
                    case e: Exception =>
                        if (running) {
                            logger.error(s"Listener error: ${e.getMessage}")
                            println(s"[ERROR] Listener error: ${e.getMessage}")
                        }
                        running = false
                }
            }
            logger.info("Listener thread stopped")
            logger.close()
            println("[LISTENER] Thread stopped")
        })
        listenerThread.start()
    }

    def stop(): Unit = {
        logger.info("Stopping listener thread")
        running = false
        if (listenerThread != null && listenerThread.isAlive) {
            listenerThread.interrupt()
            try {
                listenerThread.join(2000)
            }
            catch {
                case _: InterruptedException => logger.warn("Thread join interrupted")
            }
        }
    }


    private def handle(raw: String): Unit = {
        MessageProtocol.decode(raw) match {
            case Some(BroadcastDelivered(from, text)) =>
                logger.info(s"Broadcast from $from")
                println(s"[BROADCAST FROM $from]: $text")

            case Some(PrivateDelivered(from, to, text)) =>
                logger.info(s"Private message from $from, $to")
                println(s"[Private from $from]: $text")

            case Some(UserListMessage(users)) =>
                logger.info(s"Received user list: ${users.size} users")
                if (users.nonEmpty) {
                    println("[ONLINE USERS]")
                    users.foreach(u => println(s"  - $u"))
                }
                else {
                    println("[ONLINE USERS] No other users online")
                }

            case Some(UserJoined(user)) =>
                logger.info(s"User $user joined")
                println(s"[SYSTEM] $user joined the chat")

            case Some(UserLeft(user)) =>
                logger.info(s"User $user left")
                println(s"[SYSTEM] $user left the chat")

            case Some(WelcomeMessage(success, msg)) =>

                if (success) {
                    logger.info (s"Server message: $msg")
                    println(s"[SERVER] $msg")
                }
                else {
                    logger.warn(s"Server error: $msg")
                    println(s"[SERVER] ERROR: $msg")
                }

            case None =>
                logger.error(s"Could not decode: $raw")
                println(s"Could not decode: $raw")

            case other =>
                logger.warn(s"Unhandled message type: $other")
                println(s"Unknown: $raw")
        }
    }
}