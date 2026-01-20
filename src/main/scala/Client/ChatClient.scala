package Client
import java.util.Scanner

object ChatClient {
    def main(args: Array[String]): Unit = {
        val sc = new Scanner(System.in)
        println("Enter your username:")
        val username = sc.nextLine()

        val connection = new ClientConnection("localhost", 9090)

        val listener = new ClientListener(connection)
        val sender   = new ClientSender(connection, username)
        listener.listen()
        sender.send()
        connection.close()
    }
}