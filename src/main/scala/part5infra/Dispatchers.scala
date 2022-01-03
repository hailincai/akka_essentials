package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.util.Random

object Dispatchers extends App {
  class Counter extends Actor with ActorLogging {
    override def receive: Receive = increase(0)

    def increase(counter: Int): Receive = {
      case message => {
        val newCounter = counter + 1
        log.info(s"[$newCounter] ${message.toString}")
        context.become(increase(newCounter))
      }
    }
  }

  val system = ActorSystem("DispatchersDemo", ConfigFactory.load().getConfig("dispatchersDemo"))

  // method 1 attach dispatcher programmatically
  val counters = for (i <- 1 to 10) yield {
    system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i");
  }

  val r = new Random()
  for (i <- 1 to 1000) {
    counters(r.nextInt(10)) ! i
  }

  // method 2 attach dispatcher from config
  val simpleCounter = system.actorOf(Props[Counter], "rtjvm")

  /**
   * Dispatcher can be used as ExecutorContext
   */
  class DBActor extends Actor with ActorLogging {
    implicit val executorContext = context.system.dispatchers.lookup("my-dispatcher")

    override def receive: Receive = {
      // future block is running at the thread pool, when result ready, you can access it
      case message => Future {
        Thread.sleep(5000)
        log.info(s"Receive ${message.toString}")
      }(executorContext) // specify a special thread pool for the future, so it will not block the actor message processing
    }
  }
}
