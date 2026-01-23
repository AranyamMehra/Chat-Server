package Client
import Utility.Logger
import Utility.Utilities.{connect, reader, writer}

import java.net.Socket
import java.io.{BufferedReader, PrintWriter}
import scala.util.{Failure, Success, Try}

class ClientConnection(host: String, port: Int) {
    val socket: Socket = connect (host, port)
    val in: BufferedReader = reader(socket)
    val out: PrintWriter = writer(socket)
    private val logger = Logger("ClientConnection")

    logger.info(s"Connected to $host:$port")

    def close(): Unit = {
        List (in, out, socket).foreach(res => Try (res.close()))
        logger.info (s"Connection closed and Resources cleaned up")
    }
}