package Server
import Protocol._
import Protocol.MessageProtocol._
import Utility.Utilities.{reader, writer}
import Utility.Logger
import ServerState._

import java.io.{BufferedReader, PrintWriter}
import java.net.Socket
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class ClientHandler {
    private val logger = Logger("ClientHandler")
    var username: Option[String] = None

    def handleClient(socket: Socket): Unit = {
        val resources = for {
            in <- Try (reader(socket))
            out <- Try (writer (socket))
        } yield (in, out)

        resources match {
            case Success((in, out)) => handleClientResources(in, out, socket)

            case Failure (e) =>
                logger.error(s"Failed to create resources: ${e.getMessage}")
                closeSocket(socket)
        }
    }

    private def handleClientResources(in: BufferedReader, out: PrintWriter, socket: Socket): Unit = {
        def loop(): Future[Unit] = {
            val message = Future(in.readLine())

            message.flatMap {
                case null =>
                    logger.warn(s"Client ${username.getOrElse("unknown")} disconnected")
                    println (s"Client ${username.getOrElse("unknown")} disconnected")
                    Future.successful(())

                case rawMessage =>
                    val running = processMessage(rawMessage, out)
                    if (running)
                        loop()
                    else {
                        Future.successful(())
                    }

            }.recoverWith {
                case e: Exception =>
                    logger.error(s"Error reading from ${username.getOrElse("unknown")}: ${e.getMessage}")
                    Future.successful(())
            }
        }
        loop().onComplete {_ =>
            disconnect()
            cleanup(in, out, socket)
        }
    }

    private def processMessage(raw: String, out: PrintWriter): Boolean = {
        decode(raw) match {
            case Some(Connect(name)) =>
                username = Some(name)
                addUser(name, out)
                logger.info(s"User '${username.getOrElse("unknown")}' connected")

                out.println(ecnode(WelcomeMessage(true, s"Welcome $name")))

                broadcastExcept (ecnode(UserJoined(name)), name)

                val otherUsers = allUsers.filter(_ != name)
                out.println(ecnode(UserListMessage(otherUsers)))
                true

            // BROADCAST
            case Some(Broadcast(from, text)) =>
                logger.info(s"Broadcast from $from: $text")
                println(s"[SERVER] Broadcast from $from: $text")
                broadcastExcept (ecnode(BroadcastDelivered(from, text)), from)
                true

            // PRIVATE
            case Some(Private(from, to, text)) =>
                logger.info(s"Private message from $from to $to")
                println(s"[SERVER] Private from $from to $to: $text")
                getUser(to) match {
                    case Some(targetOut) => targetOut.println(ecnode(PrivateDelivered(from, to, text)))
                        out.println(ecnode(WelcomeMessage(true, s"Message sent to $to")))
                    println (s"Message sent to $to")

                    case None =>
                        logger.warn(s"User $to not found for private message from $from")
                        println (s"User $to not found for private message from $from")
                        out.println(ecnode(WelcomeMessage(false, s"User $to not found")))
                }
                true

            // DISCONNECT
            case Some(DisconnectMessage()) =>
                logger.info(s"User ${username.getOrElse("unknown")} requested disconnect")
                println(s"User ${username.getOrElse("unknown")} requested disconnect")
                false

            // INVALID
            case _ =>
                logger.warn(s"Invalid message from ${username.getOrElse("unknown")}: $raw")
                println(s"[SERVER] Invalid message: $raw")
                out.println(MessageProtocol.ecnode(WelcomeMessage(false, "Invalid message")))
                true
        }
    }

    private def disconnect(): Unit = {
        username.foreach { name =>
            removeUser(name) match {
                case Some (_) => broadcast(ecnode(UserLeft(name)))
                println(s"[SERVER] User $name cleaned up")
                logger.info(s"User $name disconnected and cleaned up")

                case None => logger.warn(s"User $name not found during disconnect")
            }

        }
    }

    private def cleanup(in: BufferedReader, out: PrintWriter, socket: Socket): Unit = {
        val user = username.getOrElse("unknown")
        List (in, out, socket).foreach(res => Try (res.close()))
        logger.debug(s"Resources cleaned up for $user")
        println(s"Resources cleaned up for $user")
    }

    private def closeSocket(socket: Socket): Unit = {
        Try(if (socket != null && !socket.isClosed) socket.close())
    }
}