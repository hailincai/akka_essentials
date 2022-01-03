package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {
  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.debug(message.toString)
    }
  }

  /**
   * Inline configuration
   */
  val configString =
    """
      | akka {
      |   loglevel = "INFO"
      | }
      |""".stripMargin
  val config = ConfigFactory.parseString(configString)
//  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
//  val actor = system.actorOf(Props[SimpleLoggingActor])
//  actor ! "A Message to remember"

  /***
   * load from resource file resources/application.conf
   */
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me"

  /**
   * load special config from resources/application.conf
   */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialActor ! "Special message for me"

  /**
   * load config from different config file
   */
  val seperateConfig = ConfigFactory.load("config1/config1.conf")
  println(s"log config from seperate config is: ${seperateConfig.getString("akka.loglevel")}")

  /**
   * can load from json or properties file
   */
  val jsonConfig = ConfigFactory.load("json/config.json")
  println(s"json config loglevel: ${jsonConfig.getString("akka.loglevel")}")
  println(s"json config test: ${jsonConfig.getString("test")}")
}
