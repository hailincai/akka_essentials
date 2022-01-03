package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifeCycle extends App {
  case object StartChild

  class LifecycleActor extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("I am starting...")

    override def postStop(): Unit = log.info("I have stopped...")
    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val system = ActorSystem("LifecycleDemo")
//  val parent = system.actorOf(Props[LifecycleActor], "parent")
//  parent ! StartChild
//  parent ! PoisonPill

  /**
   * Restart
   */
  case object Fail
  case object FailChild

  class Parent extends Actor {
    val child = context.actorOf(Props[Child])

    override def receive: Receive = {
      case FailChild =>
        child ! Fail
    }
  }

  class Child extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("supervised child is starting...")

    override def postStop(): Unit = log.info("supervised child is stopped...")

    // called by old instance
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"supervised child is about to restart because of ${reason.getMessage}")
    }

    // called by new instance
    override def postRestart(reason: Throwable): Unit =
      log.info("supervised child is restarted")

    override def receive: Receive = {
      case Fail =>
        log.info(s"I am gonna to fail...")
        throw new RuntimeException("fail")
    }
  }

  val supervisor = system.actorOf(Props[Parent])
  supervisor ! FailChild
}
