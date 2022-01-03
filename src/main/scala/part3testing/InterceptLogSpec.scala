package part3testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class InterceptLogSpec extends TestKit(ActorSystem("InterceptLogSpec", ConfigFactory.load().getConfig("interceptLogMessages")))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
{
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import InterceptLogSpec._
  "A checkout flow" should {
    val item = "a simple item"
    val cc = "1234-1234-1234-1234"
    val invalidCC = "0000-0000-0000-0000"

    "Correctly log out" in {
      EventFilter.info(pattern = s"Order [0-9]+ for item $item has been dispatched", occurrences = 1) intercept({
        val checkoutActor = system.actorOf(Props[CheckoutActor])
        checkoutActor ! Checkout(item, cc)
      })
    }

    "Throw exception when payment fail" in {
      EventFilter[RuntimeException](occurrences = 1) intercept({
        val checkoutActor = system.actorOf(Props[CheckoutActor])
        checkoutActor ! Checkout(item, invalidCC)
      })
    }
  }
}

object InterceptLogSpec {
  case class Checkout(item:String, creditcard: String)
  case class AuthorizeCard(creditcard: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed

  class CheckoutActor extends Actor {
    val payment = context.actorOf(Props[PaymentActor])
    val fullfillment = context.actorOf(Props[FulfillmentActor])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        payment ! AuthorizeCard(card)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fullfillment ! DispatchOrder(item)
        context.become(pendingFullfillment(item))
      case PaymentDenied => throw new RuntimeException("Got payment fail")
    }

    def pendingFullfillment(item: String): Receive = {
      case OrderConfirmed =>
        context.become(awaitingCheckout)
    }
  }

  class PaymentActor extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
        if (card.startsWith("0")) sender() ! PaymentDenied
        else {
          Thread.sleep(4000)
          sender() ! PaymentAccepted
        }
    }
  }

  class FulfillmentActor extends Actor with ActorLogging {
    var orderId = 0

    override def receive: Receive = {
      case DispatchOrder(item) =>
        orderId += 1
        log.info(s"Order $orderId for item $item has been dispatched")
        sender() ! OrderConfirmed
    }
  }
}
