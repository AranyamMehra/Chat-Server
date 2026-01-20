package Client
import Utility.Utilities.{connect, reader, writer}

import java.net.Socket
import java.io.{BufferedReader, PrintWriter}

class ClientConnection(host: String, port: Int) {
    val socket: Socket = connect (host, port)
    val in: BufferedReader = reader(socket)
    val out: PrintWriter = writer(socket)

    def close(): Unit = {
        try {
            if (out != null) {
                out.close()
            }
            if (in != null) {
                in.close()
            }
            if (socket != null && !socket.isClosed) {
                socket.close()
            }
            println("[CONNECTION] Closed successfully")
        } catch {
            case e: Exception => println(s"[CONNECTION] Error during close: ${e.getMessage}")
        }
    }
}