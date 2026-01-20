package Server
import Utility.Logger
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._

object ServerState {
    private val logger = Logger("ServerState")
    val clients = new ConcurrentHashMap[String, PrintWriter]().asScala

    def addUser(username: String, out: PrintWriter): Unit = {
        clients.put(username, out)
        logger.info(s"User '$username' added. Total users: ${clients.size}")
    }

    def removeUser(username: String): Unit = {
        logger.info(s"User '$username' removed. Total users: ${clients.size}")
        clients.remove(username)
    }

    def getUser(username: String): Option[PrintWriter] = clients.get(username)

    def allUsers: List[String] = clients.keys.toList

    def broadcast(msg: String): Unit = {
        logger.debug(s"Broadcasting to ${clients.size} users")
        clients.values.foreach(_.println(msg))
    }

    def broadcastExcept(msg: String, exceptUsername: String): Unit = {
        val count = clients.count(_._1 != exceptUsername)
        logger.debug(s"Broadcasting to $count users (except $exceptUsername)")

        clients.foreach { case (username, out) =>
            if (username != exceptUsername) {
                out.println(msg)
            }
        }
    }

}
