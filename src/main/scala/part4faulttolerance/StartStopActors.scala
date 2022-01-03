package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, Terminated}

object StartStopActors extends App {
  val system = ActorSystem("StoppingActors")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object StopSelf;
  }
  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Start child $name")
        val child = context.actorOf(Props[Child], name)
        context.become(withChildren(children + (name -> child)))
      case StopChild(name) =>
        log.info(s"Stopping child $name")
        val childOption = children.get(name)
        childOption.foreach((child) => context.stop(child)) // stop child actor
        context.become(withChildren(children - name))
      case StopSelf =>
        log.info(s"Stopping self")
        context.stop(self) // kill all children actors as well as the parent
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * Method 1: context.stop
   */
  import Parent._
//  val parent = system.actorOf(Props[Parent], "parent")
//  parent ! StartChild("child1")
//
//  val child1 = system.actorSelection("/user/parent/child1")
//  child1 ! "Hello"
//
//  parent ! StopChild("child1")

  /**
   * Method 2: Using special message
   */
//  val looseChild = system.actorOf(Props[Child])
//  looseChild ! PoisonPill // or Kill
//  looseChild ! "Are you there?"

  /**
   * Death watch
   */
  class Watcher extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = {
      case StartChild(name) =>
        log.info(s"Start child with name $name")
        val child = context.actorOf(Props[Child], name)
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"The watched child($ref) has been terminated")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("wchild")
  val child = system.actorSelection("/user/watcher/wchild")
  child ! PoisonPill
}
