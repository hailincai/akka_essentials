package exercises

import akka.actor.{Actor, ActorSystem, Props}

object Counter extends App {
  object CounterActor {
    case object INCREMENT
    case object DECREMENT
    case object PRINT
  }
  class CounterActor extends Actor {
    import CounterActor._

    override def receive: Receive = counterReceiver(0)

    def counterReceiver(currentVal: Int) : Receive = {
      case INCREMENT => context.become(counterReceiver(currentVal + 1))
      case DECREMENT => context.become(counterReceiver(currentVal - 1))
      case PRINT  => println(s"current value is $currentVal")
    }
  }

  val system = ActorSystem("actorCounter")
  val counter = system.actorOf(Props[CounterActor])
  (1 to 5).foreach(_ => counter ! CounterActor.INCREMENT)
  counter ! CounterActor.PRINT

}
