import java.net.InetSocketAddress

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success, Try}

object Main extends App {

  val app = "tcp2sctp"

  println(s"Hi\t$app [listen_host [listen_port [remote_host [remote_port]]]]")
  val def_listen_host = "localhost"
  val def_listen_port = 8008
  val def_remote_host = "127.0.0.1"
  val def_remote_port = 8009
  println(s"Where defaults are: $def_listen_host, $def_listen_port and $def_remote_host, $def_remote_port")

  val argmap = ((Stream from 1) zip args).toMap
  Try {
    (
      argmap.getOrElse(1, def_listen_host),
      argmap.get(2).map(_.toInt).getOrElse(def_listen_port),
      argmap.getOrElse(3, def_remote_host),
      argmap.get(4).map(_.toInt).getOrElse(def_remote_port)
    )
  } match {
    case Failure(_) => println("Error: Wrong arguments!")
    case Success( params @ (listen_host,listen_port,remote_host,remote_port) ) =>
      println(s"So using: $params")

      try {
        val config = ConfigFactory.parseString("akka.loglevel = DEBUG")
        implicit val system = ActorSystem(app, config)
        implicit val ec = system.dispatcher
        // implicit val materializer = ActorMaterializer()

        system.actorOf(Props(classOf[SctpClient],
          new InetSocketAddress(listen_host, listen_port),
          new InetSocketAddress(remote_host, remote_port)
        ), "sctp-client")

        println(s"$app initialization completed.")
      }
      catch {
        case _ => println("Internal error! Try to use scala version 2.11")
      }
  }
}
