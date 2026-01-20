package Server
import Utility.Logger

import java.lang.Runtime.getRuntime
import java.net.{ServerSocket, Socket}

object ChatServer {
    private val logger = Logger("ChatServer")

    private def startServer(serverSocket: ServerSocket) = {
        try {
            logger.info("Server started, waiting for connections...")

            while (true) {
                val sc: Socket = serverSocket.accept()
                println("A new Client is connected.")
                logger.info(s"New client connected")

                val client: ClientHandler = new ClientHandler

                val thread = new Thread(() => {
                    client.handleClient(sc)
                })

                thread.start()
            }
        } catch {
            case e: Exception => e.printStackTrace()
                logger.error(s"Server error: ${e.getMessage}")

        }
        finally {
            serverSocket.close()
            logger.info("Server socket closed")
            logger.close()
        }
    }

    def main(args: Array[String]): Unit = {
        val port = 9090
        val serverSocket: ServerSocket = new ServerSocket(port)

        logger.info(s"ChatServer started on port $port")
        println(s"ChatServer started on port $port")

        getRuntime.addShutdownHook(new Thread(() => {
            logger.info("Server shutting down...")
            logger.close()
        }))
        startServer (serverSocket)
    }
}