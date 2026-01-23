package Client
import Utility.Logger

import java.lang.Runtime.getRuntime
import java.util.Scanner
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.sys.addShutdownHook
import scala.util.{Failure, Success, Try}

object ChatClient {
    @volatile private var globalConnection: Option[ClientConnection] = None
    @volatile private var globalListener: Option[ClientListener] = None
    private val logger = Logger("ChatClient")

    private def addShutdownHook(): Unit = {
        sys.addShutdownHook {
            val c = performShutdownCleanup()

            Try(Await.ready(c, 1.seconds)) match {
                case Success(_) =>
                    logger.info("Shutdown hook cleanup successful")
                    println ("Shutdown hook cleanup successful")
                case Failure(e) =>
                    logger.error(s"Shutdown hook cleanup failed: ${e.getMessage}")
            }
            logger.close()
        }
    }

    private def performShutdownCleanup(): Future[Unit] = {
        logger.warn("Shutdown hook triggered - cleaning up resources...")

        for {
            _ <- Future {
                globalListener.foreach(_.stop())
                logger.debug("Listener stopped")
            }

            _ <- Future {
                globalConnection.foreach(_.close())
                logger.debug("Connection closed")
            }

        } yield {
            println("[SHUTDOWN] Complete")
        }
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

        sender.send().andThen {
            case Success(_) =>
                logger.info("User initiated shutdown")
                println("Shutting down...")

            case Failure(e) =>
                logger.error(s"Sender error: ${e.getMessage}")
        }

    }

    private def cleanup(connection: ClientConnection, listener: ClientListener): Future[Unit] = Future {
        Thread.sleep(500)
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
                println(s"Cannot connect to server: ${e.getMessage}")

            case e: Exception =>
                logger.error(s"Client error: ${e.getMessage}")
                println(s"Error: ${e.getMessage}")
                e.printStackTrace()
        }
    }

    def main(args: Array[String]): Unit = {
        addShutdownHook()

        // Get username
        print("Enter your username: ")
        val sc = new Scanner(System.in)
        val username = sc.nextLine()
        println(s"\nWelcome, $username!")
        println("Type /help for available commands\n")
        logger.info(s"Starting client for user: $username")

        val client = runClient(username)
        Await.ready(client, Duration.Inf)

        logger.info("Client shutdown complete")
        logger.close()
        println("Goodbye!")
    }
}