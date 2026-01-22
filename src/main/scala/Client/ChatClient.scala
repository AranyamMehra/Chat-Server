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

    def main(args: Array[String]): Unit = {
        val clientSession: Future[Unit] = for {
            username <- Future (askUsername())
            connection <- Future(new ClientConnection("localhost", 9090))

            _ <- Future.successful {
                setupShutdownHook(connection)
                logger.info(s"Connected as $username")
            }

            listener = new ClientListener(connection, username)
            sender = new ClientSender(connection, username)

            _ = logger.info(s"Connected as $username")

            _ <- Future.firstCompletedOf(Seq(
                listener.listen(),
                sender.send()
            ))
        } yield connection

        clientSession.onComplete {
            case Success(_) =>
                logger.info("Client shutdown complete")
                logger.close()
                println("[CLIENT] Goodbye!")

            case Failure(e: java.net.ConnectException) =>
                logger.error(s"Cannot connect to server: ${e.getMessage}")
                println(s"[CLIENT] Cannot connect to server: ${e.getMessage}")

            case Failure(e) =>
                logger.error(s"Client error: ${e.getMessage}")
                e.printStackTrace()
        }

        Try (Await.result (clientSession, Duration.Inf))
    }

    private def setupShutdownHook(conn: ClientConnection): Unit = {
        sys.addShutdownHook {
            println("\n[SHUTDOWN HOOK] Cleaning up resources...")
            conn.close()
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
}