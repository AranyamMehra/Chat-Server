package Client

import Protocol._
import Utility.Logger
import Protocol.MessageProtocol._

import java.net.SocketException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientListener (connection: ClientConnection, username: String) {
    private val logger = Logger(s"ClientListener-$username")
    @volatile var running = true

    def listen(): Future [Unit] = {
        logger.info("Starting message listener thread")
        def loop(): Future[Unit] = {
            if (! running) {
                logger.info("[LISTENER] Stop signal received, exiting loop.")
                Future.successful(())
            }
            else {
                val raw = Future(connection.in.readLine())
                raw.flatMap {
                    case null =>
                        logger.warn("[SYSTEM] Disconnected from server")
                        Future.successful(())

                    case raw => logger.debug(s"Received: $raw")
                        handle(raw)
                        loop()
                }.recover {
                    case e: Exception => logger.error(s"[ERROR] Listener error: ${e.getMessage}")
                }
            }
        }
        loop ()
    }

    def stop(): Unit = {
        logger.info("Stopping listener thread")
        running = false
    }

    private def handle(raw: String): Unit = {
        decode(raw) match {
            case Some(BroadcastDelivered(from, text)) =>
                logger.info(s"[BROADCAST FROM $from]: $text")

            case Some(PrivateDelivered(from, to, text)) =>
                logger.info(s"[Private from $from to $to]: $text")

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

            case Some(UserLeft(user)) =>
                logger.info(s"User $user left")

            case Some(WelcomeMessage(success, msg)) =>
                if (success) {
                    logger.info (s"Server message: $msg")
                }
                else {
                    logger.warn(s"Server error: $msg")
                }

            case None =>
                logger.error(s"Could not decode: $raw")

            case other =>
                logger.warn(s"Unhandled message type: $other")
        }
    }
}