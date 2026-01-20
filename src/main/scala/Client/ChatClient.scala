package Client
import Utility.Logger
import java.lang.Runtime.getRuntime
import java.util.Scanner

object ChatClient {
    @volatile private var globalConnection: ClientConnection = _
    @volatile private var globalListener: ClientListener = _
    private val logger = Logger("ChatClient")

    def main(args: Array[String]): Unit = {

        getRuntime.addShutdownHook(new Thread(() => {
            logger.warn("Shutdown hook triggered - cleaning up resources...")
            println("\n[SHUTDOWN HOOK] Cleaning up resources...")

            if (globalListener != null) {
                globalListener.stop()
            }

            if (globalConnection != null) {
                globalConnection.close()
            }
            logger.info("Cleanup complete")
            logger.close()
            println("[SHUTDOWN HOOK] Cleanup complete")
        }))

        val sc = new Scanner(System.in)
        println("Enter your username: ")
        val username = sc.nextLine()
        logger.info(s"Starting client for user: $username")

        var connection: ClientConnection = null
        var listener: ClientListener = null
        var sender: ClientSender = null

        try {
            connection = new ClientConnection("localhost", 9090)
            globalConnection = connection
            logger.info("Connected to server successfully")

            listener = new ClientListener(connection, username)
            globalListener = listener

            sender = new ClientSender(connection, username)

            listener.listen()
            sender.send()

            println("[CLIENT] Shutting down...")
            logger.info("User initiated shutdown")
            Thread.sleep(500)

        } catch {
            case e: java.net.ConnectException =>
                logger.error(s"Cannot connect to server: ${e.getMessage}")
                println(s"[CLIENT] Cannot connect to server: ${e.getMessage}")

            case e: Exception =>
                logger.error(s"Client error: ${e.getMessage}")
                println(s"[CLIENT] Error: ${e.getMessage}")
                e.printStackTrace()
        }
        finally {
            if (listener != null) {
                listener.stop()
            }
            if (connection != null) {
                connection.close()
            }
            globalConnection = null
            globalListener = null

            logger.info("Client shutdown complete")
            logger.close()
            println("[CLIENT] Goodbye!")
        }
    }
}