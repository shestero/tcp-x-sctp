import akka.util.ByteString

object AkkaMessages {

  case class TCPClose(conn:Int)

  case class SctpFail() // TODO: general upstream fail
  case class SctpClose(conn: Int)

  case class Up(  conn:Int, data: ByteString)
  case class Down(conn:Int, data: ByteString)
}
