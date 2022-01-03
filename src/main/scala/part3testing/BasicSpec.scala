package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.DurationInt
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
{
  // setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._

  "A simple actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello"
      echoActor ! message

      expectMsg(message) // timeout defined in akka.test.single-expect-default
    }
  }

  "A blackhold actor" should {
    "send bac some mssage" in {
      val blackHole = system.actorOf(Props[Blackhold])
      val message = "hello"
      blackHole ! message

      expectNoMessage(1 second)
    }
  }

  "A lab test actor should" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into uppercase" in {
      labTestActor ! "I love akka"
      val reply = expectMsgType[String]
      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("Hi", "Hello")
    }

    "reply with fav tech" in {
      labTestActor !"favTech"
      expectMsgAllOf("Scala", "Akka")

      labTestActor ! "favTech"
      val messages = receiveN(2) // Seq[Any]
      assert(messages.length == 2)

      labTestActor ! "favTech"
      expectMsgPF() {
        case "Scala" =>
        case "Akka" =>
      }
    }
  }
}

object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class Blackhold extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()

    override def receive: Receive = {
      case "favTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case "greeting" => if (random.nextBoolean()) sender() ! "Hi" else sender() ! "Hello"
      case message: String => sender() ! message.toUpperCase()
    }
  }
}

