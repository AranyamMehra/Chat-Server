package Client
import Protocol._
import Protocol.MessageProtocol._
import Utility.Logger
import java.util.Scanner
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientSender (connection: ClientConnection, username: String) {
    private val sc = new Scanner(System.in)
    private val logger = Logger(s"ClientSender-$username")

    def send(): Future [Unit] = {
        connection.out.println(ecnode(Connect(username)))

        def loop(): Future[Unit] = {
            val input = Future (sc.nextLine())
            input.flatMap {
                case "/quit" =>
                    connection.out.println(ecnode(DisconnectMessage()))
                    logger.info("User requested disconnect")
                    Future.successful(())
                case text => processInput (text)
                    loop ()
                case _ => Future.successful (())
            }
        }
        loop ()
    }

    private def processInput(input: String): Unit = {
        input match {
            case a if (input.startsWith ("/all")) =>connection.out.println(ecnode(Broadcast(username, input.drop(5))))

            case b if (input.startsWith("/pm ")) =>
                val parts = input.split(" ", 3)
                if (parts.length == 3) {
                    connection.out.println(ecnode(Private(username, parts(1), parts(2))))
                }
                else {
                    println("Usage: /pm <user> <message>")
                }
            case _ =>  println("Unknown command")
        }
    }
}