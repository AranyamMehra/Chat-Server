package Server
import Protocol._
import Protocol.MessageProtocol._
import Server.ServerUtilities.{reader, writer}
import java.io.PrintWriter

import java.net.Socket
import java.util._

class ClientHandler {
    var username: Option[String] = None
    def handleClient(socket: Socket): Unit = {
        val in  = reader(socket)
        val out = writer(socket)

        var running = true
        while (running) {
            val rawMessage = in.readLine()
            if (rawMessage == null) {
                println("Client disconnected unexpectedly")
                running = false
            }
            else {
                val continue = processMessage(rawMessage, out)
                running = continue
            }
        }

        in.close()
        out.close()
        socket.close()
        println("Client connection closed cleanly")
    }

    def processMessage(raw: String,  out: PrintWriter): Boolean = {
        MessageProtocol.decode(raw) match {
            case Some(Connect(name)) =>
                username = Some(name)
                ServerState.addUser(name, out)

                out.println(MessageProtocol.ecnode(WelcomeMessage(true, s"Welcome $name")))

                ServerState.broadcast( ecnode(UserJoined(name)))
                out.println(MessageProtocol.ecnode(UserListMessage(ServerState.allUsers)))
                true

            // BROADCAST
            case Some(Broadcast(from, text)) =>
                ServerState.broadcast(MessageProtocol.ecnode(BroadcastDelivered(from, text)))
                true

            // PRIVATE
            case Some(Private(from, to, text)) =>
                ServerState.getUser(to) match {
                    case Some(targetOut) => targetOut.println(MessageProtocol.ecnode(PrivateDelivered(from, to, text)))

                    case None => out.println(MessageProtocol.ecnode(WelcomeMessage(false, s"User $to not found")))
                }
                true

            // DISCONNECT
            case Some(DisconnectMessage()) => disconnect()
                false

            // INVALID
            case _ =>
                out.println(MessageProtocol.ecnode(WelcomeMessage(false, "Invalid message")))
                true
        }
    }

    private def disconnect(): Unit = {
        username.foreach { name =>
            ServerState.removeUser(name)
            ServerState.broadcast(ecnode(UserLeft(name)))
        }
    }

}
