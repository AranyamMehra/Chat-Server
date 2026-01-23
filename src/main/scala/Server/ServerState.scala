package Server
import Protocol.{MessageProtocol, ServerShutdown}
import Protocol.MessageProtocol.ecnode
import Utility.Logger

import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters._
import scala.util.Try

object ServerState {
    private val logger = Logger("ServerState")
    val clients = new ConcurrentHashMap[String, PrintWriter]().asScala

    def addUser(username: String, out: PrintWriter): Unit = {
        clients.put(username, out)
        logger.info(s"User '$username' added. Total users: ${clients.size}")
    }

    def removeUser(username: String): Option[PrintWriter] = {
        val remove = clients.remove(username)

        remove match {
            case Some(x) => logger.info(s"User '$username' removed. Total users: ${clients.size}")
                Some(x)
            case None =>
                logger.warn(s"Attempted to remove non-existent user '$username'")
                None
        }
    }

    def getUser(username: String): Option[PrintWriter] = clients.get(username)

    def allUsers: List[String] = clients.keys.toList

    def broadcast(msg: String): Unit = {
        logger.debug(s"Broadcasting to ${clients.size} users")
        clients.values.foreach(_.println(msg))
    }

    def broadcastExcept(msg: String, other: String): Unit = {
        val count = clients.count(_._1 != other)
        logger.debug(s"Broadcasting to $count users (except $other)")

        clients.foreach { case (username, out) =>
            if (username != other) {
                out.println(msg)
            }
        }
    }

    def broadcastServerShutdown(): Unit = {
        logger.info(s"Broadcasting server shutdown to ${clients.size} clients")
        val shutdownMsg = ecnode(ServerShutdown())

        clients.values.foreach { out =>
            Try {
                out.println(shutdownMsg)
                out.flush()
            }
        }
    }
}
