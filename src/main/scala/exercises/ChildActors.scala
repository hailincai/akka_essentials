package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {
  // Distributed word counting
  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) => {
        val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCouterWorker], s"wcw_$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
      }
    }

    def withChildren(childrenRefs: Seq[ActorRef], currChildIdx: Int, currTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] I have received: $text - I will send to child $currChildIdx with taskId $currTaskId")
        val task = WordCountTask(currTaskId, text)
        val child = childrenRefs(currChildIdx)
        val newRequestMap = requestMap + (currTaskId -> sender())
        child ! task
        context.become(withChildren(childrenRefs, (currChildIdx + 1) % (childrenRefs.length), currTaskId + 1, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] I have received a reply for task id $id with $count")
        val sender = requestMap(id)
        sender ! count
        context.become(withChildren(childrenRefs, currChildIdx, currTaskId, requestMap - id))
    }
  }

  class WordCouterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) => {
        println(s"${self.path} I have received task: $id, $text")
        sender ! WordCountReply(id, text.split(" ").length)
      }
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val messages = List("Hello Akka world", "I am in good mood")
        messages.foreach((message) => master ! message)
      case count =>
        println(s"[TestActor] Receive a word count reply: $count")
    }
  }

  val system = ActorSystem("wordCountTest")
  val test = system.actorOf(Props[TestActor], "testActor")
  test ! "go"
}
