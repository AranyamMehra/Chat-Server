package Utility

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket

object Utilities {
    def reader(socket: Socket): BufferedReader =
        new BufferedReader(new InputStreamReader(socket.getInputStream))

    def writer(socket: Socket): PrintWriter =
        new PrintWriter(socket.getOutputStream, true)

    def connect(host: String, port: Int): Socket = {
        val socket = new Socket(host, port)
        println("Connected to server")
        socket
    }
}
