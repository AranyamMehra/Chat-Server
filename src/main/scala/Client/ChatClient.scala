package Client
import Utility.Logger

import java.lang.Runtime.getRuntime
import java.util.Scanner
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

object ChatClient {
    @volatile private var globalConnection: Option[ClientConnection] = None
    @volatile private var globalListener: Option[ClientListener] = None
    private val logger = Logger("ChatClient")

    private def setupShutdownHook(): Unit = {
        sys.addShutdownHook {
            globalListener.foreach(_.stop())
            globalConnection.foreach(_.close())
            logger.close()
        }
    }

    private def askUsername() = {
        val sc = new Scanner(System.in)
        println("Enter your username: ")
        val username = sc.nextLine()
        logger.info(s"Starting client for user: $username")
        username
    }

    private def createConnection(): Future[ClientConnection] = Future {
        val connection = new ClientConnection("localhost", 9090)
        globalConnection = Some(connection)
        logger.info("Connected to server successfully")
        connection
    }

    private def startListener(connection: ClientConnection, username: String): Future[ClientListener] = {
        val listener = new ClientListener(connection, username)
        globalListener = Some(listener)
        listener.listen()
        logger.info("Listener started")
        Future.successful(listener)
    }

    private def startSender(connection: ClientConnection, username: String): Future[Unit] = {
        val sender = new ClientSender(connection, username)
        sender.send()
    }

    private def cleanup(connection: ClientConnection, listener: ClientListener): Future[Unit] = Future {
        logger.info("Cleaning up resources...")
        listener.stop()
        connection.close()
        globalConnection = None
        globalListener = None
        logger.info("Cleanup complete")
    }

    def runClient(username: String): Future [Unit] = {
        val clientSession: Future[Unit] = for {
            connection <- createConnection()
            listener <- startListener(connection, username)
            _ <- startSender(connection, username)
            _ <- cleanup(connection, listener)

        } yield ()

        clientSession.recover {
            case e: java.net.ConnectException =>
                logger.error(s"Cannot connect to server: ${e.getMessage}")
                println(s"[CLIENT] Cannot connect to server: ${e.getMessage}")

            case e: Exception =>
                logger.error(s"Client error: ${e.getMessage}")
                println(s"[CLIENT] Error: ${e.getMessage}")
                e.printStackTrace()
        }
    }

    def main(args: Array[String]): Unit = {
        setupShutdownHook()
        val username =  askUsername()
        logger.info(s"Starting client for user: $username")
        val clientFuture = runClient(username)
        Await.ready(clientFuture, Duration.Inf)
        logger.info("Client shutdown complete")
        logger.close()
        println("[CLIENT] Goodbye!")
    }
}