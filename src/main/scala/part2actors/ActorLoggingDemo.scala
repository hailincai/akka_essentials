package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {
  // #1 explicitly define a logger
  class SimpleActorWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)

    override def receive: Receive = {
      case message => logger.info(message.toString)
    }
  }

  // #2 using trait ActorLogging
  class ActorWithLogginTrait extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Receive two things: {} and {}", a, b)
    }
  }

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger], "sActorWithLogger")
  actor ! "Hello World"

  val actor1 = system.actorOf(Props[ActorWithLogginTrait], "sActorWithTrait")
  actor1 ! ("Hello", "Akka")
}
