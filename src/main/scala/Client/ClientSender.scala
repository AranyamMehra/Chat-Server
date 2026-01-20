package Client
import Protocol._
import Protocol.MessageProtocol._
import java.util.Scanner

class ClientSender (connection: ClientConnection, username: String) {
    private val sc = new Scanner(System.in)

    def send(): Unit = {
        connection.out.println(ecnode(Connect(username)))

        var running = true
        while (running) {
            val input = sc.nextLine()

            if (input.startsWith("/all ")) {
                connection.out.println(ecnode(Broadcast(username, input.drop(5))))

            }
            else if (input.startsWith("/pm ")) {
                val parts = input.split(" ", 3)
                if (parts.length == 3) {
                    connection.out.println(ecnode(Private(username, parts(1), parts(2))))
                }
                else {
                    println("Usage: /pm <user> <message>")
                }

            }
            else if (input == "/quit") {
                connection.out.println(ecnode(DisconnectMessage()))
                running = false

            }
            else {
                println("Unknown command")
            }
        }
    }

}
