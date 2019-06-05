import java.net.InetSocketAddress
import java.nio.charset.Charset

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString

import AkkaMessages._


class SimplisticHandler(conn:Int) extends Actor with ActorLogging {
  import Tcp._
  def receive = {
    case Received(data) =>
      println(s"SimplisticHandler Received ${data.size} byte(s) for #$conn")
      context.parent ! Tcp2Sctp(conn, data)

    case PeerClosed     =>
      println(s"closed $conn")
      context.parent ! TCPClose(conn)
      context.stop(self)
  }
}


class TCPServer(listen: InetSocketAddress) extends Actor with ActorLogging {
  import Tcp._
  import context.system

  var pool = scala.collection.parallel.mutable.ParMap[Int,ActorRef]()

  println("Starting the server...")
  IO(Tcp) ! Bind(self, listen)

  def receive = {
    case b @ Bound(localAddress) =>
      println(s"TCPServer: Bound to $localAddress")
      context.parent ! b

    case CommandFailed(_: Bind) =>
      println("CommandFailed")
      context.stop(self)

    case c @ Connected(remoteAddresses, localAddresses) =>
      println(s"TCPServer: new connection accepted from $remoteAddresses")
      val conn = remoteAddresses.getPort
      val connection = sender()
      val handler = context.actorOf(Props(classOf[SimplisticHandler],conn), "tcp_"+conn )
      pool.put(conn,connection)
      connection ! Register(handler)
      // TODO: signal to SCTP of assign the chanel $conn

    case msg @ TCPClose(conn) =>
      println(s"Remote close connection #$conn")
      pool-=conn
      context.parent.forward(msg) // TODO: signal into SCTP of free the chanel $conn

    case Sctp2Tcp(conn, data) =>
      pool.get(conn) match {
        case Some(c) => c ! Write(data)
        case None => println(s"Error! No connection/wrong channel #$conn")
      }

    case t @ Tcp2Sctp(conn, data) =>
      println(s"forwarded @ #$conn data.size=${data.size}")
      context.parent ! t // forward
  }
}
