package Client
import Client.ClientUtilities._

import java.net.Socket
import java.io.{BufferedReader, PrintWriter}

class ClientConnection(host: String, port: Int) {
    val socket: Socket = new Socket(host, port)
    val in: BufferedReader = reader(socket)
    val out: PrintWriter = writer(socket)

    def close(): Unit = {
        socket.close()
    }
}