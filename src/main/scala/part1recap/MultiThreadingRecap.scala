package part1recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultiThreadingRecap extends App {
  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("I'm running in parallel")
  })
  aThread.start();
  aThread.join();

  val threadHello = new Thread(() => (1 to 1000).foreach(_ => println("hello")))
  val threadGoodbye = new Thread(() => (1 to 1000).foreach(_ => println("goodbye")))
  threadHello.start()
  threadGoodbye.start()
  threadHello.join()
  threadGoodbye.join()

  // different runs produce different results
  class BankAccount(private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int) = this.amount -= money
    def safeWithdraw(money: Int) = this.synchronized {
      this.amount -= money;
    }

    // inter-thread communication on the JVM
    // wait - notify mechanism

    // Scala Futures
    import scala.concurrent.ExecutionContext.Implicits.global
    val futures = Future {
      // long computation
      42
    }

    // callbacks
    futures.onComplete {
      case Success(value42) => println("something")
      case Failure(_) => println("something happen")
    }

    val aProcessedFuture = futures.map(_ + 1)
    val aFaltFuture = futures.flatMap {
      value => Future(value + 2)
    }
    val filteredFuture = futures.filter(_ % 2 == 0) // NoSuchElementException
    // for comprehensions
    val aNonsenseFuture = for {
      meaningOfLife <- futures
      filteredMeaning <- filteredFuture
    }yield meaningOfLife + filteredMeaning

    // andThen, recover / recoverWith

    // Promises
  }
}
