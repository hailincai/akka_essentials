package part1recap

import scala.concurrent.Future

object ThreadModelLimitations extends App {
  /*

   */

  /**
   * #1: OOP encapsulation is only valid in the SINGLE THREADED MODEl
   */
  class BankAccount(private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int) = this.amount -= money
    def deposit(money: Int): Unit = this.amount += money
    def getAmount = amount
  }
//
//  val account = new BankAccount(2000)
//  for (_ <- 1 to 1000) {
//    new Thread(() => account.withdraw(1)).start()
//  }
//  for (_ <- 1 to 1000){
//    new Thread(() => account.deposit(1)).start()
//  }
  // OOP encapsulation is broken in a multithreaded env
  // synchronization! locks to the rescure
  // may get deadlocks, livelocks

  /**
   * #2: delegating something to a thread is a PAIN
   */
  // you have a running thread and you want to pass a runnable to that thread
  var task: Runnable = null
  val runningThread: Thread = new Thread(() => {
    while ( true ){
      while (task == null) {
        runningThread.synchronized {
          println("background waiting for a task...")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("background I have a task!")
        task.run()
        task = null
      }
    }
  })

  def delegateToBGThread (r: Runnable) = {
    if (task == null) task = r

    runningThread.synchronized {
      runningThread.notify()
    }
  }

  runningThread.start()
  Thread.sleep(500)
  delegateToBGThread(() => println(42))
  Thread.sleep(1000)
  delegateToBGThread(() => println("this should run in the backgound"))

  /**
   * #3: tracing and dealing with errors in a multiplethreaded env is a PITN.
   */
  // 1M numbers is between 10 threads
  import scala.concurrent.ExecutionContext.Implicits.global
  val futures = (0 to 9)
    .map(i => 100000 *i until 100000* (i + 1))
    .map(range => Future {
      if (range.contains(546735)) throw new RuntimeException("invalid number")
      range.sum
    })
  val sumFuture = Future.reduceLeft(futures)(_ + _)
  sumFuture.onComplete(println)
}
