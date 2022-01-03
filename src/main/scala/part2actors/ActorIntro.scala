package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorIntro extends App {
  // part1 - actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // part2 - create actors
  // first word count actor
  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behavior
    // Receive ia s PartialFunction[Any, Unit]
    override def receive: Receive = {
      case message: String =>
        println(s"[word counter]I have received: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I cannot understand ${msg.toString}")
    }

  }

  // part3 - instantiate our actor
  // Props[WordCountActor] is calling Props.apply[T]()
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  // part4 - communicate
  wordCounter ! "I am learning Akka and it's pretty damn cool!" // "tell"
  anotherWordCounter ! "A different message"
  // asynchronous

  object Person {
    def props(name: String): Props = Props[Person](new Person(name))
  }

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"my name is $name")
      case _ => println(s"I don't understand the verb")
    }
  }
  val personActor = actorSystem.actorOf(Person.props("Bob"))
  personActor ! "Hi"
}
