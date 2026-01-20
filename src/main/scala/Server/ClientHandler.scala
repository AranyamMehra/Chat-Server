package Server
import Protocol._
import Protocol.MessageProtocol._
import Utility.Utilities.{reader, writer}

import java.io.{BufferedReader, PrintWriter}
import java.net.Socket

class ClientHandler {
    var username: Option[String] = None

    def handleClient(socket: Socket): Unit = {
        var in: BufferedReader = null
        var out: PrintWriter = null

        try {
            in = reader(socket)
            out = writer(socket)

            var running = true
            while (running) {
                try {
                    val rawMessage = in.readLine()
                    if (rawMessage == null) {
                        println(s"[SERVER] Client ${username.getOrElse("unknown")} disconnected")
                        disconnect()
                        running = false
                    }
                    else {
                        val continue = processMessage(rawMessage, out)
                        running = continue
                    }
                } catch {
                    case e: java.net.SocketException =>
                        println(s"[SERVER] Socket error for ${username.getOrElse("unknown")}: ${e.getMessage}")
                        disconnect()
                        running = false
                    case e: java.io.IOException =>
                        println(s"[SERVER] IO error for ${username.getOrElse("unknown")}: ${e.getMessage}")
                        disconnect()
                        running = false
                    case e: Exception =>
                        println(s"[SERVER] Unexpected error for ${username.getOrElse("unknown")}: ${e.getMessage}")
                        e.printStackTrace()
                        running = false

                }
            }
        } catch {
            case e: Exception =>
                println(s"[SERVER] Fatal error handling client: ${e.getMessage}")
                e.printStackTrace()
        } finally {
            cleanup (in, out, socket)
        }

        in.close()
        out.close()
        socket.close()
        println("Client connection closed cleanly")
    }

    private def processMessage(raw: String, out: PrintWriter): Boolean = {
        MessageProtocol.decode(raw) match {
            case Some(Connect(name)) =>
                username = Some(name)
                ServerState.addUser(name, out)

                out.println(MessageProtocol.ecnode(WelcomeMessage(true, s"Welcome $name")))

                ServerState.broadcastExcept ( ecnode(UserJoined(name)), name)
                val otherUsers = ServerState.allUsers.filterNot(_ == name)
                out.println(ecnode(UserListMessage(otherUsers)))
                true

            // BROADCAST
            case Some(Broadcast(from, text)) =>
                println(s"[SERVER] Broadcast from $from: $text")
                ServerState.broadcastExcept(ecnode(BroadcastDelivered(from, text)), from)
                true

            // PRIVATE
            case Some(Private(from, to, text)) =>
                println(s"[SERVER] Private from $from to $to: $text")
                ServerState.getUser(to) match {
                    case Some(targetOut) => targetOut.println(ecnode(PrivateDelivered(from, to, text)))
                        out.println(ecnode(WelcomeMessage(true, s"Message sent to $to")))

                    case None => out.println(ecnode(WelcomeMessage(false, s"User $to not found")))
                }
                true

            // DISCONNECT
            case Some(DisconnectMessage()) => disconnect()
                false

            // INVALID
            case _ =>
                println(s"[SERVER] Invalid message: $raw")
                out.println(MessageProtocol.ecnode(WelcomeMessage(false, "Invalid message")))
                true
        }
    }

    private def disconnect(): Unit = {
        username.foreach { name =>
            ServerState.removeUser(name)
            ServerState.broadcast(ecnode(UserLeft(name)))
            println(s"[SERVER] User $name cleaned up")
        }
    }

    private def cleanup(in: java.io.BufferedReader, out: PrintWriter, socket: Socket): Unit = {
        try {
            if (in != null) {
                in.close()
            }

            if (out != null) {
                out.close()
            }

            if (socket != null && !socket.isClosed) {
                socket.close()
            }
        } catch {
            case e: Exception => println(s"[CONNECTION] Error during close: ${e.getMessage}")
        }

        println(s"[SERVER] Resources cleaned up for ${username.getOrElse("unknown")}")
    }
}