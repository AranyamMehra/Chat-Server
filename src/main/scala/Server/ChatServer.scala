package Server
import Utility.Logger

import java.lang.Runtime.getRuntime
import java.net.{ServerSocket, Socket}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, blocking, Promise}
import scala.util.{Failure, Success, Try}

object ChatServer {
    private val logger = Logger("ChatServer")
    val keepAlive = Promise[Unit]()

    private def startServer(serverSocket: ServerSocket): Try [Unit] = Try {
        logger.info("Server started, waiting for connections...")
        println("Server started, waiting for connections...")

        def connection(): Unit = {
            val sc = Future{
                blocking {
                    serverSocket.accept()
                }
            }

            sc.onComplete {
                case Success(clientSocket) =>
                    handleNewClient(clientSocket)
                    connection()
                case Failure(e) =>
                    logger.error(s"Error accepting client: ${e.getMessage}")
            }
        }
        connection()
    }

    private def handleNewClient(clientSocket: Socket): Unit = {
        Future {
            val client = new ClientHandler
            client.handleClient (clientSocket)
        }.onComplete {
            case Success(_) => logger.info(s"New client connected")
                            println ("New Client connected")
            case Failure (e) => logger.error(s"Server error: ${e.getMessage}")
        }
    }

    def main(args: Array[String]): Unit = {
        val port = 9090
        val serverSocket: ServerSocket = new ServerSocket(port)

        logger.info(s"ChatServer started on port $port")
        println(s"ChatServer started on port $port")

        getRuntime.addShutdownHook(new Thread(() => {
            logger.info("Server shutting down - notifying clients...")
            ServerState.broadcastServerShutdown()
            Try(serverSocket.close())
            logger.info("Server shutdown complete")
            logger.close()

        }))

        startServer (serverSocket) match {
            case Success(_) =>
                Await.result(keepAlive.future, Duration.Inf)

            case Failure(e) =>
                logger.error(s"Server failed: ${e.getMessage}")
                e.printStackTrace()
        }
        Try(serverSocket.close())
        logger.info("Server shutdown complete")
        println ("Server shutdown !!!!!")
        logger.close()
    }
}