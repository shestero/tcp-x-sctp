import akka.actor._
import akka.io._
import java.net.InetSocketAddress

import akka.util.Bytes

import scala.concurrent.duration._
import AkkaMessages._

class SctpServer(listen: InetSocketAddress, remote: InetSocketAddress) extends Actor with ActorLogging {
  import Sctp._
  //implicit val system = context.system
  import context.system

  case class Ack(message: SctpMessage) extends Event

  val maxStreams = 65535
  IO(Sctp) ! Bind(self, listen, maxStreams, maxStreams)

  def receive = {
    case Bound(localAddresses, port) => println(s"SCTP server bound to $localAddresses . Listening port $port ...")
    case Connected(remoteAddresses, localAddresses, association) =>
      println(s"new connection accepted from $remoteAddresses assoc=${association.id}")
      sender ! Register(self, Some(self)) // TODO: new handler with TCPClient for every new connection !!
      println("registred...")

      val up = context.actorOf(Props(classOf[TCPClient], remote) ) // , "tcp-client")

      // see also: context.parent
      val connection = sender
      context.become({
        case Down(conn, data) =>
          println(s"Tcp2Sctp($conn, data.size=${data.size})")
          val msg = SctpMessage(Bytes(data.asByteBuffer), streamNumber=conn)
          connection ! Send(msg, Ack(msg)) // ?

        case Received(SctpMessage(SctpMessageInfo(streamNumber, payloadProtocolID, timeToLive, unordered, bytes, association, address), payload)) =>
          println(s"received $bytes bytes from $address on stream #$streamNumber with protocolID=$payloadProtocolID and TTL=$timeToLive and assoc=${association.id}")
          up ! Up(streamNumber,payload.toByteString)

        // case x=> println("we have got something... ???:"+x)

        case cc: ConnectionClosed =>
          println("SCTP Connection closed. Reason: "+cc.getErrorCause)
          context.unbecome() // dangerous!

      }, discardOld = false)

      // TODO disconnect

    case Ack(msg) =>
      //println(s"message sent back")
      println(s"SctpClient: ACK: message sent with ${msg.payload.size} bytes on stream #${msg.info.streamNumber}")

    case n: Notification => println("Notification: "+n)
    case msg => println("SctpServer message: "+msg)
  }

}
