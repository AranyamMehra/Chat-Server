package Client
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket

object ClientUtilities {
    def reader(socket: Socket): BufferedReader =
        new BufferedReader(new InputStreamReader(socket.getInputStream))

    def writer(socket: Socket): PrintWriter =
        new PrintWriter(socket.getOutputStream, true)

    def shutdown(socket: Socket): Unit = {
        socket.close()
        println("Client shut down")
    }
}