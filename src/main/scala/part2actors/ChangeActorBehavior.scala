package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangeActorBehavior.Mom.MomStart

object ChangeActorBehavior extends App {
  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._
    var state = HAPPY

    override def receive: Receive = {
      case Food(VEGETALE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETALE) =>  context.become(sadReceive)// change receive handler to sad
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETALE) =>
      case Food(CHOCOLATE) => context.become(happyReceive)// change receive handler to happy
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // do you want to play?
    val VEGETALE = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETALE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("Yay, my kid is happy!")
      case KidReject => println("My kid is sad, but he's healty")
    }
  }

  val system = ActorSystem("changingActorBehavior")
  val mom = system.actorOf(Props[Mom])
  val kid = system.actorOf(Props[FussyKid])
  val kid1 = system.actorOf(Props[StatelessFussyKid])

  mom ! MomStart(kid1)

  /**
   * context.become(<new handler>, true/false)
   *    if true, then te receive handler will be replaced
   *    otherwise, will stack the new handler at the top of old handler
   */
  /**
   * context.unbecome() pop out the latest handler
   */

  /**
   * Excercuses
   * 1 - recreate the Counter Actor with context.become with no mutable state
   * 2 - Simplified voting system
   */

}
