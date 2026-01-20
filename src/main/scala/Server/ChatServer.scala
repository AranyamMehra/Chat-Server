package Server
import java.net.{ServerSocket, Socket}

object ChatServer {
    private def startServer(serverSocket: ServerSocket) = {
        try {
            while (true) {
                val sc: Socket = serverSocket.accept();
                println("A new Client is connected.");
                val client: ClientHandler = new ClientHandler

                val thread = new Thread(() => {
                    client.handleClient(sc)
                })

                thread.start();
            }
        } catch {
            case e: Exception => e.printStackTrace();
        }
        finally {
            serverSocket.close()
        }
    }

    def main(args: Array[String]): Unit = {
        val port = 9090
        val serverSocket: ServerSocket = new ServerSocket(port)
        println(s"ChatServer started on port $port")
        startServer (serverSocket)
    }
}