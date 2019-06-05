import akka.util.ByteString

object AkkaMessages {

  case class TCPClose(conn:Int)

  case class SctpFail() // TODO: general upstream fail
  case class SctpClose(conn: Int)

  case class Sctp2Tcp(conn:Int, data: ByteString)
  case class Tcp2Sctp(conn:Int, data: ByteString)
}
