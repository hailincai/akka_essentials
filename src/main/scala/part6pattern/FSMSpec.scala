package part6pattern

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest, WordSpecLike}

import scala.concurrent.duration._

class FSMSpec extends TestKit(ActorSystem("FSMSpec"))
with ImplicitSender with WordSpecLike with BeforeAndAfterAll with OneInstancePerTest {
  override def afterAll():Unit = TestKit.shutdownActorSystem(system)

  import FSMSpec._

  "A vending machine" should {
    runSpecTests(Props[VendingMachine])
  } //end of tst suite

  "Vending machine FSM" should {
    runSpecTests(Props[VendingMachineFSM])
  }

  def runSpecTests(props: Props) = {
    "error when not initialized" in {
      val vm = system.actorOf(props)
      vm ! RequestProduct("coke")
      expectMsg(VendingError("MachineNotInitialized"))
    }

    "report a product not available" in {
      val vm = system.actorOf(props)
      vm ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vm ! RequestProduct("sanwich")
      expectMsg(VendingError("ProductNotvailable"))
    }

    "throw timeout if I don't insert money" in {
      val vm = system.actorOf(props)
      vm ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vm ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 1 dollars"))

      within(1.5 seconds) {
        expectMsg(VendingError("RequestTimeOut"))
      }
    }

    "handle the reception of partial money" in {
      val vm = system.actorOf(props)
      vm ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vm ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
      vm ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 2 dollars"))
      within(1.5 seconds) {
        expectMsg(VendingError("RequestTimeOut"))
        expectMsg(GiveBackChange(1))
      }
    }

    "delivery product is full money paid" in {

      val vm = system.actorOf(props)
      vm ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vm ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
      vm ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }

    "should deliver and give back change if paid too much" in {
      val vm = system.actorOf(props)
      vm ! Initialize(Map("coke" -> 10), Map("coke" -> 3))
      vm ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
      vm ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 2 dollars"))
      vm ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))

      vm ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))
    }

  }
}

object FSMSpec {
  /**
   * Vending machine
   */
  case class Initialize(inventory: Map[String, Int], prices: Map[String, Int])
  case class RequestProduct(product: String)
  case class Instruction(instruction: String) // message the VM will show on screen
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)

  case class VendingError(reason: String)
  case object ReceiveMoneyTimeout

  class VendingMachine extends Actor with ActorLogging{
    implicit val executionContext = context.dispatcher
    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(inventory, prices) => context.become(operational(inventory, prices))
      case _ => sender() ! VendingError("MachineNotInitialized")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => {
        inventory.get(product) match {
          case None | Some(0) => sender() ! VendingError("ProductNotvailable")
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert ${price} dollars")
            context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
        }
      }
    }

    def waitForMoney(inventory: Map[String, Int],
                     prices: Map[String, Int],
                     product: String,
                     money: Int,
                     moneyTimeoutSchedule: Cancellable,
                     requester: ActorRef): Receive = {
      case ReceiveMoneyTimeout => {
        requester ! VendingError("RequestTimeOut")
        if (money > 0) requester ! GiveBackChange(money) // XXXXX
        context.become(operational(inventory, prices))
      }
      case ReceiveMoney(amount) => {
        moneyTimeoutSchedule.cancel()
        val price = prices(product)
        if (money + amount >= price){
          // user bought product
          requester ! Deliver(product)
          if (money + amount - price > 0){
            requester ! GiveBackChange(money + amount - price)
          }
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          context.become(operational(newInventory, prices))
        }else{
          val remaining = price - money - amount
          requester ! Instruction(s"Please insert ${remaining} dollars")
          context.become(waitForMoney(inventory,
            prices,
            product,
            money + amount,
            startReceiveMoneyTimeoutSchedule, requester))
        }
      }
    }

    def startReceiveMoneyTimeoutSchedule =
      context.system.scheduler.scheduleOnce(1 second){
        self ! ReceiveMoneyTimeout
      }
  }

  // step 1 - define state and data
  trait VendingState
  case object Idle extends VendingState
  case object Operational extends VendingState
  case object WaitForMoney extends VendingState

  trait VendingData
  case object Uninitialized extends VendingData
  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
  case class WaitForMoneyData(inventory: Map[String, Int],
                              prices: Map[String, Int],
                              product: String,
                              money: Int,
                              requester: ActorRef) extends VendingData
  class VendingMachineFSM extends FSM[VendingState, VendingData] {
    // we don't have a receive handler

    // when message in, trigger EVENT(message, data)
    // handle state and EVENT

    startWith(Idle, Uninitialized)

    when(Idle) {
      case Event(Initialize(inventory, prices), Uninitialized) =>
        goto(Operational) using Initialized(inventory, prices)
      case _ =>
        sender() ! VendingError("MachineNotInitialized")
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialized(inventory, prices)) =>
        inventory.get(product) match {
          case None | Some(0) =>
            sender() ! VendingError("ProductNotvailable")
            stay()
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert ${price} dollars")
            goto(WaitForMoney) using WaitForMoneyData(inventory, prices, product, 0, sender())
        }

    }

    when (WaitForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitForMoneyData(inventory, prices, _, money, requester)) => {
        requester ! VendingError("RequestTimeOut")
        if (money > 0) requester ! GiveBackChange(money) // XXXXX
        goto(Operational) using Initialized(inventory, prices)
      }
      case Event(ReceiveMoney(amount), WaitForMoneyData(inventory, prices, product, money, requester)) => {
        val price = prices(product)
        if (money + amount >= price){
          // user bought product
          requester ! Deliver(product)
          if (money + amount - price > 0){
            requester ! GiveBackChange(money + amount - price)
          }
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          goto(Operational) using Initialized(newInventory, prices)
        }else{
          val remaining = price - money - amount
          requester ! Instruction(s"Please insert ${remaining} dollars")
          stay() using WaitForMoneyData(inventory, prices, product, money + amount, requester)
        }
      } //end of ReeiveMoney event
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError("CommandNotFound")
        stay()
    }

    onTransition {
      case stateA -> stateB => log.info(s"Transitioning from ${stateA} to ${stateB}")
    }

    initialize() // this really start the actor
  }
}
