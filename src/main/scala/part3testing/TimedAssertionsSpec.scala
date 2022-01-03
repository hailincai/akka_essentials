package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}

import scala.concurrent.duration._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.util.Random

class TimedAssertionsSpec extends TestKit(ActorSystem("TimedAssertionsSpec"))
with ImplicitSender
with WordSpecLike
with BeforeAndAfterAll
{
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import TimedAssertionsSpec._

  "Worker actor" should {
    val worker = system.actorOf(Props[Worker])
    "reply with meaningful of life in timely manner" in {
      within(500 millis, 1 second) {
        worker ! "work"
        expectMsg(WorkResult(42))

        // TestProbe not follow the within timeout, using akka.test.single-expect-default instead
      }
    }

      "reply with valid work in reasonable seq" in {
        within(2 seconds) {
          worker ! "workSeq"

          val received: Seq[Int] = receiveWhile[Int](max = 2 seconds, idle = 500 millis, messages=10){
            case WorkResult(result) => result
          }

          assert(received.length > 5)
        }
      }
  }
}

object TimedAssertionsSpec {
  case class WorkResult(result: Int)

  class Worker extends Actor {
    override def receive: Receive = {
      case "work" =>
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSeq" =>
        val randon = new Random()
        (1 to 10).foreach((_) => {
          Thread.sleep(randon.nextInt(50))
          sender() ! WorkResult(1)
        })
    }
  }
}
