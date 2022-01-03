package part1recap

import scala.concurrent.Future

object AdvanceRecap extends App {
  // partial functions ( subset from dmain )
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42;
    case 2 => 65;
    case 5 => 999
  }

  val pf = (x: Int) => x match {
    case 1 => 42;
    case 2 => 65;
    case 5 => 999
  }

  val function: (Int => Int) = partialFunction
  val modifiedList = List(1, 2, 3).map {
    case 1 => 42
    case _ => 0
  }

  // lifting
  // become total function Int => Option[Int]
  val lifted = partialFunction.lift

  // orElse
  val pfChain = partialFunction.orElse[Int, Int] {
    case 60 => 9000
  }
  pfChain(5) // 999 per partialFunction
  pfChain(60) // 9000 from pfChain

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  def received: ReceiveFunction = {
    case 1 => println("hello")
    case _ => println("something else...")
  }

  // implicits
  implicit val timeout = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int)= f()
  setTimeout(() => println("aaa")) // extra parameter omitted

  // implicit conversions
  // 1) implicit defs
  case class Person(name: String) {
    def greet = s"Hi, my name is $name"
  }
  implicit def fromStringToPerson(string: String): Person = Person(string)
  println("Peter".greet)
  // 2) implicit classes
  implicit class Dog(name: String) {
    def bark = println("bark!")
  }
  "Lassie".bark
  // organize
  // local scope
  implicit val inverseOrdering: Ordering[Int] =
    Ordering.fromLessThan(_ > _)
  List(1, 2, 3).sorted // List(3, 2, 1)
  // imported scope
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    println("Hello, future!")
  }
  // companion objects of the types included in the call
  object Person {
    implicit val personOrdering: Ordering[Person] =
      Ordering.fromLessThan((a, b) => a.name.compareTo(b.name) < 0)
  }
  List(Person("Bob"), Person("Alice")).sorted
  // List(Person("Alice"), Person("Bob"))
}
