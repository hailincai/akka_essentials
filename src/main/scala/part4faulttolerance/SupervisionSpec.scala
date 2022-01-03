package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  override def afterAll():Unit = TestKit.shutdownActorSystem(system)

  import SupervisionSpec._

  "A supervisor" should {
    "resume its child in case a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCount]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "Akka is awesome because I am learning to think in a whole new way"
      child ! Report
      expectMsg(3)
    }

    "restart its child in case of empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCount]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! ""
      child ! Report
      expectMsg(0)
    }

    "supervisor should terminate child in case of major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCount]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! "akka is nice"
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }

    "esclate an error when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCount]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! 42
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "A kind supervisor" should {
    "not kill children in case it is restarted" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "supervisor")
      supervisor ! Props[FussyWordCount]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 45
      child ! Report // child not restarted, but internal state is gone
      expectMsg(0)
    }
  }

  "all for one supervisor" should {
    "restart all children" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor])
      supervisor ! Props[FussyWordCount]
      val child1 = expectMsgType[ActorRef]
      supervisor ! Props[FussyWordCount]
      val child2 = expectMsgType[ActorRef]

      child2 ! "Hello Akka"
      child2 ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept({
        child1 ! ""
      })

      Thread.sleep(500)
      child2 ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec {
  case object Report

  class Supervisor extends Actor {
    // one for one strategy only apply to the child which throws error
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException => Restart // ActorRef no change, but internal instance change
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val child = context.actorOf(props)
        sender() ! child
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {}
  }

  class AllForOneSupervisor extends Supervisor {
    // same strategy apply to all children, not just the child throwing error
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart // ActorRef no change, but internal instance change
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class FussyWordCount extends Actor {
    var count = 0

    override def receive: Receive = {
      case Report => sender() ! count
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("sentence is too big")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else count += sentence.split(" ").length
      case _ => throw new Exception("only support string")
    }
  }
}
