package Client
import java.lang.Runtime.getRuntime
import java.util.Scanner

object ChatClient {
    @volatile private var globalConnection: ClientConnection = _
    @volatile private var globalListener: ClientListener = _

    def main(args: Array[String]): Unit = {

        getRuntime.addShutdownHook(new Thread(() => {
            println("\n[SHUTDOWN HOOK] Cleaning up resources...")
            if (globalListener != null) {
                globalListener.stop()
            }
            if (globalConnection != null) {
                globalConnection.close()
            }
            println("[SHUTDOWN HOOK] Cleanup complete")
        }))

        val sc = new Scanner(System.in)
        println("Enter your username: ")
        val username = sc.nextLine()

        var connection: ClientConnection = null
        var listener: ClientListener = null
        var sender: ClientSender = null

        try {
            connection = new ClientConnection("localhost", 9090)
            globalConnection = connection

            listener = new ClientListener(connection)
            globalListener = listener

            sender = new ClientSender(connection, username)

            listener.listen()
            sender.send()

            println("[CLIENT] Shutting down...")
            Thread.sleep(500)

        } catch {
            case e: java.net.ConnectException =>
                println(s"[CLIENT] Cannot connect to server: ${e.getMessage}")
            case e: Exception =>
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
            println("[CLIENT] Goodbye!")
        }

    }
}