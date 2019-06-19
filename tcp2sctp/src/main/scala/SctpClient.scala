import akka.actor._
import akka.io._
import java.net.InetSocketAddress

import akka.util.{ByteString, Bytes}

import scala.concurrent.duration._
import AkkaMessages._

class SctpClient(listen: InetSocketAddress, remote: InetSocketAddress) extends Actor with ActorLogging {
  import Sctp._

  //import context.system
  implicit val system = context.system
  import system.dispatcher

  case class Ack(message: SctpMessage) extends Event

  val sctpio = IO(Sctp)
  val maxStreams = 65535
  sctpio ! Connect(remote, maxStreams, maxStreams)

  println(s"SctpClient trying connect ...")

  def receive: Receive = {
    case Connected(remoteAddresses, localAddresses, association) =>
      println(s"SctpClient: set connection to $remoteAddresses assoc=${association.id}")
      println(s"!! maxOutboundStreams=${association.maxOutboundStreams}")

      val down = context.actorOf(Props(classOf[TCPServer],listen), "tcp-server")

      val connection = sender
      connection ! Register(self, Some(self))

      context.become({
        case Up(conn, data) => // note that this message should by send after SCTP is connected !
          println(s"SctpClient sending Tcp2Sctp($conn, data.size=${data.size})")
          val msg = SctpMessage(Bytes(data.asByteBuffer), streamNumber=conn)
          connection ! Send(msg, Ack(msg))

        case Received(message) =>
          down ! Down(message.info.streamNumber, message.payload.toByteString)
          println(s"SctpClient received ${message.payload.size} byte(s) from #${message.info.streamNumber}")

      }, discardOld = false)
      // TODO disconnect

    case Ack(msg) =>
      println(s"SctpClient: ACK: message sent with ${msg.payload.size} bytes on stream #${msg.info.streamNumber}")

    case CommandFailed(cmd: Connect) => system.scheduler.scheduleOnce(5.seconds, sctpio, cmd) // reconnect?

    case n: Notification => println("SctpClient notification: "+n)
    case msg => println("SctpClient message: "+msg)
  }
}
