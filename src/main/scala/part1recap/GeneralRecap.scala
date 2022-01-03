package part1recap

import scala.util.Try

object GeneralRecap extends App {
  val aCondition: Boolean = false

  var aVariable = 42
  aVariable += 1

  // expressions
  val aConditionVal = if (aCondition) 42 else 65

  // code block
  val aCodeBlock = {
    if (aCondition) 74
    56
  }

  // types
  // Unit -- only has side effect
  val theUnit = println("Hello, Scala")

  def aFunction(x: Int) = x + 1

  // recursion - TAIL recursion
  def factorial(n: Int, acc: Int): Int =
    if (n <= 0) acc
    else factorial(n - 1, acc * n)

  // OOP
  class Animal
  class Dog extends Animal
  val aDog: Animal = new Dog

  trait Canivore {
    def eat(a: Animal): Unit
  }

  class Crocodile extends Animal with Canivore {
    override def eat(a: Animal): Unit = println("Yammi")
  }

  // method notations
  val aCroc = new Crocodile
  aCroc eat aDog

  // generics
  abstract class MyList[+A]
  // companion objects
  object MyList

  // case classes
  case class Person(name: String, age: Int) // a LOT in the course!

  // Exception
  val aPoentialFailure = try{
    throw new RuntimeException("aaa")
  }catch {
    case e: Exception => "I caught an exception"
  }finally{

  }

  // functional programming
  // FP is all about working with functions as first-class
  List(1, 2, 3).map(x => x + 1)

  // for comprehensions
  val pairs = for {
    num <- List(1, 2, 3)
    c <- List('a', 'b', 'c')
  } yield num + "_" + c

  // Seq, Array, List, Vector, Map, Tuples, Sets

  // "collections"
  // Option and Try
  val anOption = Some(2)
  val aTry = Try {
    throw new RuntimeException
  }

  // pattern matching
  val unknown = 2
  val order = unknown match {
    case _ => "unknown"
  }

  // ALL THE PATTERNS
}
