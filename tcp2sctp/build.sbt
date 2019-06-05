
name := "tcp2sctp"
description := "TCP-to-SCTP gate"
version := "0.1"

scalaVersion := "2.11.12"


libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.+"

// https://github.com/arturopala/akka-io-sctp
resolvers += Resolver.jcenterRepo
libraryDependencies ++= Seq("me.arturopala" %% "akka-io-sctp" % "0.8")

mainClass in (Compile, run) := Some("Main")

enablePlugins(AssemblyPlugin)
//unmanagedJars ++= Seq()
