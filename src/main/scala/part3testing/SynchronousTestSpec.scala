package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration.Duration

class SynchronousTestSpec extends WordSpecLike with BeforeAndAfterAll{
  implicit val system = ActorSystem("SychronousTestSpec")

  override def afterAll():Unit = system.terminate()

  import SynchronousTestSpec._

  "A counter" should {
    "sychronously increase number" in {
      val counter = TestActorRef[Counter] // a shychronous actor ref, the actor get message in the same thread
      counter ! Inc

      assert(counter.underlyingActor.counter == 1)
    }

    "work on the calling thread dispatcher" in {
      // we don't need to use TestActorRef, instead of we can specify to use calling thread dispatcher
      // so actor will get message in the dispatching thread, it means excute immediately
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      val probe = TestProbe()

      probe.send(counter, Read)
      probe.expectMsg(Duration.Zero, 0) // because calling thread, so probe doesn't need to wait any time
    }
  }
}

object SynchronousTestSpec {
  case object Inc
  case object Read

  class Counter extends Actor {
    var counter = 0

    override def receive: Receive = {
      case Inc => counter += 1
      case Read => sender() ! counter
    }
  }
}
