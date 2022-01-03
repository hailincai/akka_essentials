package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {
  class SimpleActor extends Actor{
    // context is a very important attribute in the Actor
    // context.self points to SimpleActor itself
    override def receive: Receive = {
      case "Hi" => context.sender() ! "Hello, there!" // replay to a message
      case message: String => println(s"[${context.self}] I have received $message")
      case number: Int => println(s"[simple actor] I have received a NUMBER: $number")
      case SpecialMessage(content) => println(s"[simple actor] I have received a special message: $content")
      case SendMessageToYourself(content) =>
        self ! content
      case SayHiTo(ref) => ref ! "Hi"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // keep origin sender
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleactor")

  simpleActor ! "hello, actor"

  // 1 - messages can be of any type
  // a) message must be IMMUTABLE
  // b) message must be SERIALIZABLE
  // in pratice use case classes and case objects
  simpleActor ! 42 // if there is no sender, sender will be deadLetters

  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")

  // 2 - actors have information about their context and about themselves
  // context.self === sefl === 'this' in oop
  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am proud of myself")

  // 3 - actors can REPLY to messages
  // context.sender() is who send the message
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hi" // Hi will call sender() !, and sender is null, message goes to dead letters

  // 5 -- forwarding messages
  // D -> A -> B
  // forwarding = sending a meesage with the ORIGINAL sender
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob) // based on code, when bob get the message, the sender is null, not the alice

  /**
   * Exercises
   * 1. create a counter actor
   *    - Increment
   *    - Decrement
   *    - Print
   * 2. a Bank account as an actor
   *    - deposit amount, reply Succ / Failure
   *    - withdraw amount , reply Succ / Failure
   *    - Statement
   *    interact with some other kind of actor
   */
}
