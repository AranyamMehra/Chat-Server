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
                logger.info("Stop signal received, exiting loop.")
                Future.successful(())
            }
            else {
                val raw = Future(connection.in.readLine())
                raw.flatMap {
                    case null =>
                        logger.warn("[SYSTEM] Disconnected from server")
                        println ("[SYSTEM ERROR] Disconnected from server / SERVER DIED (RIP!!)")
                        Future.successful(())

                    case raw => logger.debug(s"Received: $raw")
                        handle(raw)
                        loop()
                }.recoverWith {
                    case e: SocketException if !running =>
                        logger.info("Socket closed (expected during shutdown)")
                        Future.successful(())

                    case e: SocketException =>
                        logger.error(s"Connection error: ${e.getMessage}")
                        println("[ERROR] Lost connection to server")
                        Future.successful(())

                    case e: Exception if running =>
                        logger.error(s"Listener error: ${e.getMessage}")
                        println(s"\n[ERROR] Unexpected error: ${e.getMessage}")
                        Future.successful(())

                    case _ =>
                        Future.successful(())
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
                logger.info(s"Broadcast from $from: $text")
                println(s"[$from to ALL] $text")


            case Some(PrivateDelivered(from, to, text)) =>
                logger.info(s"[Private from $from]: $text")
                println(s"[$from to YOU] $text")

            case Some(UserListMessage(users)) =>
                logger.info(s"Received user list: ${users.size} users")
                if (users.nonEmpty) {
                    println(s"[ONLINE] ${users.mkString(", ")}")
                }

            case Some(UserJoined(user)) =>
                logger.info(s"User $user joined")
                println(s"$user joined the CHAT")


            case Some(UserLeft(user)) =>
                logger.info(s"User $user left")
                println(s"[-] $user GONE AWOL")

            case Some(WelcomeMessage(success, msg)) =>
                if (success) {
                    logger.info (s"Server message: $msg")
                }
                else {
                    logger.warn(s"Server error: $msg")
                    println(s"[ERROR] $msg")
                }

            case Some(ServerShutdown()) =>
                logger.warn("Server is shutting down")
                println("[SERVER] Server is shutting down. You will no longer be able to message.")
                running = false

            case None =>
                logger.error(s"Could not decode: $raw")

            case other =>
                logger.warn(s"Unhandled message type: $other")
        }
    }
}