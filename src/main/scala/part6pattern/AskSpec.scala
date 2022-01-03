package part6pattern

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll{
  override def afterAll():Unit = TestKit.shutdownActorSystem(system)

  import AskSpec._

  "An Authenticator" should {
    import AuthManager._

    "fail to authenticate non register user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! AuthenticateUser("test", "abc")
      expectMsg(AuthFail(AuthManager.AUTH_FAIL_NOT_FOUND))
    }

    "fail to autheticate for invalid pwd" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("test", "pwd")
      authManager ! AuthenticateUser("test", "123")
      expectMsg(AuthFail(AuthManager.AUTH_FAIL_WRONG_PWD))
    }

    "success authenticate for valid u/p" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("test", "pwd")
      authManager ! AuthenticateUser("test", "pwd")
      expectMsg(AuthSucc)
    }
  }

  "An Piped Authenticator" should {
    import AuthManager._

    "fail to authenticate non register user" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! AuthenticateUser("test", "abc")
      expectMsg(AuthFail(AuthManager.AUTH_FAIL_NOT_FOUND))
    }

    "fail to autheticate for invalid pwd" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("test", "pwd")
      authManager ! AuthenticateUser("test", "123")
      expectMsg(AuthFail(AuthManager.AUTH_FAIL_WRONG_PWD))
    }

    "success authenticate for valid u/p" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("test", "pwd")
      authManager ! AuthenticateUser("test", "pwd")
      expectMsg(AuthSucc)
    }
  }
}

object AskSpec {
  case class Read(key: String)
  case class Write(key: String, value: String)
  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) => {
        log.info(s"Try to read [${key}]")
        sender() ! kv.get(key)
      }
      case Write(key, value) => {
        log.info(s"Try to set value [${value}] for key [${key}]")
        context.become(online(kv + (key -> value)))
      }
    }
  }

  case class RegisterUser(name: String, password: String)
  case class AuthenticateUser(name: String, password: String)
  case class AuthFail(message: String)
  case object AuthSucc

  object AuthManager {
    val AUTH_FAIL_NOT_FOUND = "Not found"
    val AUTH_FAIL_WRONG_PWD = "WRONG PWD"
    val AUTH_FAIL_SYSTEM_ERROR = "SYSTEM ERROR"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    implicit val timeout: Timeout = Timeout(3 second)
    implicit val executionContext = context.dispatcher

    val authDb = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(name, password) => authDb ! Write(name, password)
      case AuthenticateUser(name, password) => {
        handleAuthentication(name, password)
      }
    } // end of receive

    def handleAuthentication(user: String, pwd: String) = {
      val originalSender = sender()
      val future = authDb ? Read(user)
      future.onComplete({
        // this code is called in another thread
        // NEVER CALL ACTOR METHODS OR ACCESS MUTABLE STATES IN ONCOMPLETE
        // avoid closing over the actor instance or mutable state
        case Success(None) => originalSender ! AuthFail(AUTH_FAIL_NOT_FOUND)
        case Success(Some(dbPwd)) => {
          if (dbPwd == pwd) originalSender ! AuthSucc
          else originalSender ! AuthFail(AUTH_FAIL_WRONG_PWD)
        }
        case Failure(_) => originalSender ! AuthFail(AUTH_FAIL_SYSTEM_ERROR)
      })
    }
  }

  class PipedAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuthentication(user: String, pwd: String): Unit = {
      val future = authDb ? Read(user)
      val passwordFuture = future.mapTo[Option[String]]
      val responseFuture = passwordFuture.map({
        case None => AuthFail(AUTH_FAIL_NOT_FOUND)
        case Some(dbPwd) => {
          if (dbPwd == pwd) AuthSucc
          else AuthFail(AUTH_FAIL_WRONG_PWD)
        }
      })

      responseFuture.pipeTo(sender())
    }
  }
}
