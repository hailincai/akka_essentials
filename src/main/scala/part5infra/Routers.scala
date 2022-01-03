package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

/**
 * Support Routing logic
 * - Round Robin
 * - Random
 * - Smallest mailbox ( who has less message in the queue)
 * - boradcast
 * - scatter-gather-first
 * - tail-chopping
 * - consistent hashing
 */
object Routers extends App {
  /*
  Method 1 manually route
   */
  class Master extends Actor {
    // step 1: create routee
    val workers = for (i <- 1 to 5) yield {
      val worker = context.actorOf(Props[Worker], s"worker_$i")
      context.watch(worker)
      ActorRefRoutee(worker)
    }

    // step 2: create router
    val router = Router(RoundRobinRoutingLogic(), workers)

    override def receive: Receive = {
      //step 3: handle worker die
      case Terminated(ref) => {
        router.removeRoutee(ref)
        val newWorker = context.actorOf(Props[Worker])
        context.watch(newWorker)
        router.addRoutee(newWorker)
      }
      // step4 route message to worker
      case message => {
        router.route(message, sender())
      }

    }
  }

  class Worker extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  // testing
  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])
//  for (i <- 1 to 10) {
//    master ! s"[$i] Hello World"
//  }

  /**
   * Method 2, Using pool
   */
  // 2.1 Programmatically create the pool
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "simpleRRPool")

  // 2.2 Creating pool from configuration
  val poolMasters = system.actorOf(FromConfig.props(Props[Worker]), "poolMaster2")

  /**
   * Method 3
   *  Group master, the worker is created somewhere else, and group them to a pool
   */
  val workers = (1 to 5).map(i => system.actorOf(Props[Worker], s"worker_$i"))
  val workerPaths = workers.map(worker => worker.path.toString)
  // 3.1 in code
  val groupMaster = system.actorOf(RoundRobinGroup(workerPaths).props(), "groupMaster1")
  // 3.2 in configuration ( I don't think it is a good idea, because you need to know the worker path ahead )
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  groupMaster2 ! Broadcast("Hello World!") // everyone got this special message

  // PoisionPill and Kill not be routed
  // AddRoutee / Remove / Get only be handled by router
}
