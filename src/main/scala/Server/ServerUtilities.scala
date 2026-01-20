package Server
import Protocol.{Connect, MessageProtocol}

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}

object ServerUtilities {
    def reader(socket: Socket): BufferedReader =
        new BufferedReader(new InputStreamReader(socket.getInputStream))

    def writer(socket: Socket): PrintWriter =
        new PrintWriter(socket.getOutputStream, true)

    def shutdown(serverSocket: ServerSocket, clientSocket: Socket): Unit = {
        clientSocket.close()
        serverSocket.close()
        println("Server shut down")
    }

    def connect(host: String, port: Int): Socket = {
        val socket = new Socket(host, port)
        println("Connected to server")
        socket
    }

    def sendConnect(out: PrintWriter, username: String): Unit = {
        val msg = MessageProtocol.ecnode(Connect(username))
        out.println(msg)
    }

    def readResponse(in: BufferedReader): Unit = {
        val response = in.readLine()
        println(s"Server says: $response")
    }
}
