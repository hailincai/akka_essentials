package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestProbSpec extends TestKit(ActorSystem("TestProbSpec"))
with ImplicitSender
with WordSpecLike
with BeforeAndAfterAll
{

  override def afterAll():Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbSpec._

  "maser actor" should {
    "register a slave" in {
      // test probe is a stub actor can be assertion
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegisterAck)
    }

    "send the work to the slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegisterAck)

      val workloadString = "I love akka"
      master ! Work(workloadString)
      slave.expectMsg(SlaveWork(workloadString, testActor))
      // instruct slave send back message
      slave.reply(WorkCompleted(3, testActor))
      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegisterAck)

      val workloadString = "I love akka"
      master ! Work(workloadString)
      master ! Work(workloadString)

      slave.receiveWhile() {
        case SlaveWork(`workloadString`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }


      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbSpec {
  case class Register(slaveRef: ActorRef)
  case object RegisterAck
  case class Work(text: String)
  case class SlaveWork(text: String, originalRequester: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Report(totalWordCount: Int)

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender() ! RegisterAck
        context.become(online(slaveRef, 0))
    }

    def online(slaveRef: ActorRef, totalWordCount: Int) :Receive = {
      case Work(text) =>
        slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalCount = totalWordCount + count
        originalRequester ! Report(newTotalCount)
        context.become(online(slaveRef, newTotalCount))
    }
  }
}
