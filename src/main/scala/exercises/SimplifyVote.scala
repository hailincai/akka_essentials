package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object SimplifyVote {
  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor{
    override def receive: Receive = {
      case Vote(candidate) => context.become(voted(candidate))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    def voted(c: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(c))
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
        context.become(awatingStatus(citizens, Map()))
    }

    def awatingStatus(stillWaiting: Set[ActorRef], currentState: Map[String, Int]): Receive = {
      case VoteStatusReply(None) =>
        // not voted yet
        sender() ! VoteStatusRequest // this might end up in an infinite loopc
      case VoteStatusReply(candidate) =>
        val newStillWaiting = stillWaiting - sender()
        val curentVoteOfCandidate = currentState.getOrElse(candidate.get, 0)
        val newCurrentState = currentState + (candidate.get -> (curentVoteOfCandidate + 1))
        if (newStillWaiting.isEmpty){
          println(s"[aggregator] poll stats: $newCurrentState")
        }else{
          context.become(awatingStatus(newStillWaiting, newCurrentState))
        }
    }
  }

  val system = ActorSystem("vote")
  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Janas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))
}
