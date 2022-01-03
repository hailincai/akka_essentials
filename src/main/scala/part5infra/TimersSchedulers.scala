package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration.DurationInt

object TimersSchedulers extends App {
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo");
  val simpleActor = system.actorOf(Props[SimpleActor], "task");

  import system.dispatcher; // after this, you will have an implict ExecutionContext in the runtime
  system.log.info("Schedule reminder in one sec...")
  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "Reminder"
  }//(system.dispatcher)

  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds){
    simpleActor ! "HeartBeat"
  }

  system.scheduler.scheduleOnce(5 seconds) {
    routine.cancel();
  }

  /**
   * Excersie: Self closing actor
   *  when actor receive any message, a time out window been created.
   *  If not receive another message during window, actor quit itself
   */
  class SelfClosingActor extends Actor with ActorLogging {
    override def receive: Receive = myReceive(null)

    def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second){
        self ! "timeout"
      }
    }

    def myReceive(timeout: Cancellable): Receive = {
      case "timeout" => {
        log.info("timeout, stopping myself...")
        context.stop(self)
      }
      case message => {
        if (timeout != null) timeout.cancel()
        val newTimeout = createTimeoutWindow()
        context.become(myReceive(newTimeout))
      }
    }
  }

  /**
   * Timer
   */
  case object TimerKey
  case object Start
  case object HeartBeat
  case object Stop
  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start => {
        log.info("Start the periodic timer...")
        timers.startPeriodicTimer(TimerKey, HeartBeat, 1 second)
      }
      case HeartBeat => {
        log.info("Receive heart beat message...")
      }
      case Stop => {
        log.info("Try to stop myself...")
        timers.cancel(TimerKey) // need to cancel the timer to avoid memory leak
        context.stop(self)
      }
    }
  }
}
