package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedControlAwareMailbox, UnboundedPriorityMailbox}
import com.typesafe.config.Config

object Mailboxes extends App {
  val system = ActorSystem("MailboxDemo")

  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  /**
   * Case I: Prority mailbox
   * P0 -> most important
   * P1 P2 P3
   */
  // step 1, create the mailbox type
  class SuppportTicketPriorityMailBox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator {
        case message: String if message.startsWith("[P0]") => 0
        case message: String if message.startsWith("[P1]") => 1
        case message: String if message.startsWith("[P2]") => 2
        case message: String if message.startsWith("[P3]") => 3
        case _ => 4
      }
    )
  // step 2, in configuration, put the mailbox for a dispatcher
  // step 3, attach the dispatcher to the actor
  val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
  supportTicketActor ! "[P3] P3 TICKET"
  supportTicketActor ! "[P2] P3 TICKET"
  supportTicketActor ! "[P1] P3 TICKET"
  supportTicketActor ! "[P0] P3 TICKET"
  supportTicketActor ! "OTHER TICKET"
  // After which time can I send another message be prioritized ( ans is no control )

  /**
   * Case 2 control-aware mailbox
   *    Use UnboundedControlAwareMailbox
    */
  // step 1 - mark important message as control messages
  case object ManagementTicket extends ControlMessage
  // step 2 - configure who gets the mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
  controlAwareActor ! "[P1] P3 TICKET"
  controlAwareActor ! "[P0] P3 TICKET"
  controlAwareActor ! ManagementTicket
}
