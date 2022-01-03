package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {
  // Actors can create other actors

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor {
    import Parent._

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        // create a new actor
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) =>
        if (childRef != null) childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  import Parent._
  val system = ActorSystem("parentchilddemo")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("Hey kid!")

  // actor hierarcies
  // parent -> child -> grandChild
  //        -> child2 -> ...
  /*
  Guardian actors ( top level )
   - /system = system guardian
   - /user   = user-level guardian ( system.actorOf owned by it )
   - /       = root guardian ( manage both /system and /user )
   */
  /**
   * Actor selection
   */
  val childSelection = system.actorSelection("/user/parent/child")
  childSelection ! "I found you!"

  /**
   * Danger!
   *
   * NEVER PASS MUTABLE ACTOR STATE, OR THE "THIS" REFERENCE TO CHILD ACTORS
   *
   * NEVER IN YOUR LIFE
   *
   * BREAK THE ACTOR PRINCIPAL, only through message
   */
  object NaiveBanckAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }
   class NaiveBanckAccount extends Actor {
     import NaiveBanckAccount._
     import CreditCard._
     var amount = 0
    override def receive: Receive = {
      case InitializeAccount =>
        val ccRef = context.actorOf(Props[CreditCard])
        ccRef ! AttachToAccount(this) // !!
      case Deposit(funds) => amount += funds
      case Withdraw(funds) => withdraw(funds)
    }
     def withdraw(funds: Int) = amount -= funds
  }

  object CreditCard {
    case class AttachToAccount(bank: NaiveBanckAccount) // !!
    case object CheckStatus
  }
  class CreditCard extends Actor {
    import CreditCard._
    override def receive: Receive = {
      case AttachToAccount(acct) => context.become(attachedTo(acct))
    }

    def attachedTo(account: NaiveBanckAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed.")
        account.withdraw(1) // sync issue will happen
    }
  }
}
