package Server
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

object ServerState {
    val clients = new ConcurrentHashMap[String, PrintWriter]().asScala

    def addUser(username: String, out: PrintWriter): Unit = clients.put(username, out)

    def removeUser(username: String): Unit = clients.remove(username)

    def getUser(username: String): Option[PrintWriter] = clients.get(username)

    def allUsers: List[String] = clients.keys.toList

    def broadcast(msg: String): Unit = clients.values.foreach(_.println(msg))

    def broadcastExcept(msg: String, exceptUsername: String): Unit = {
        clients.foreach { case (username, out) =>
            if (username != exceptUsername) {
                out.println(msg)
            }
        }
    }

}
