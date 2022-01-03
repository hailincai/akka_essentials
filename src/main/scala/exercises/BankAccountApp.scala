package exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import exercises.BankAccountApp.Person.LiveMessage

object BankAccountApp extends App {
  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {
    import BankAccount._

    private var funds = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Deposit amount is negative")
        else {
            funds += amount
            sender() ! TransactionSuccess(s"New balance is ${funds}")
        }
      case Withdraw(amount) =>
        if (amount < 0) sender() ! TransactionFailure("Withdraw amount is negative")
        else if (amount > funds) sender() ! TransactionFailure("Can't withdraw more than funds")
        else{
          funds -= amount
          sender() ! TransactionSuccess(s"New balance is ${funds}")
        }
      case Statement => sender() ! s"You account balance is ${funds}"
    }
  }

  object Person {
    case class LiveMessage(account: ActorRef)
  }

  class Person extends Actor {
    import Person._
    import BankAccount._
    override def receive: Receive = {
      case LiveMessage(account) =>
        account ! Deposit(10)
        account ! Withdraw(5)
        account ! Statement
      case message => println(message.toString)
//      case TransactionFailure(reason) =>
//        println(s"txn fail because of $reason")
//      case TransactionSuccess(message) =>
//        println(s"txn succ, message is: $message")
    }
  }

  val system = ActorSystem("bankAccount")
  val acct = system.actorOf(Props[BankAccount])
  var per = system.actorOf(Props[Person])
  per ! LiveMessage(acct)
}
