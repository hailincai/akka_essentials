package part6pattern

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

/**
 * Not stash same message twice, will throw error
 * Stash trait override the preRestart, need to mix-in if you want to override preRestart
 */
object StashMessageDemo extends App {
  /**
   * ResourceActor
   *  - when is open, it will accept read/write to resource
   *  - otherwise, will postpone the read/write until is open
   *
   * ResourceActor is closed when startup
   *  - Open ==> switch to open state
   *  - Read / Write message are POSTPONSED
   *
   * ResourceActor is open
   *  - Read / Write are handled
   *  - Close switch to close state
   */
  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  class ResourceActor extends Actor with ActorLogging with Stash {
    var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open => {
        log.info("Switch to open state...")
        unstashAll()
        context.become(open)
      }
      case message => {
        log.info(s"Stashing [${message.toString}] because can't handle it in closing state")
        stash()
      }
    }

    def open: Receive = {
      case Read => {
        log.info(s"I have read inner data: $innerData")
      }
      case Write(data) => {
        log.info(s"Write ${data} to innerData")
        innerData = data
      }
      case Close => {
        log.info("Switch to close state...")
        unstashAll()
        context.become(closed)
      }
      case message => {
        log.warning(s"Stashing message [${message.toString}] because can't handle it in open state")
        stash()
      }
    }
  }

  val system = ActorSystem("StashDemo")
  val actor = system.actorOf(Props[ResourceActor])

  actor ! Read
  actor ! Open
  actor ! Open
  actor ! Write("something")
  actor ! Close
  actor ! Read
}
