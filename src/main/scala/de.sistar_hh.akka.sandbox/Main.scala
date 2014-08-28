package de.sistar_hh.akka.sandbox

import akka.actor.Actor.Receive
import akka.actor._
import akka.routing.ConsistentHashingRouter.ConsistentHashable

object Main {
  def main(args: Array[String]) {
    val actorName: String = classOf[Master].getName



    val system = ActorSystem("mySystem")
    val myActor = system.actorOf(Props[Master], "masterRouter")
    myActor ! new Work {}
  }

}

final case class Evict(key: String)

final case class Get(key: String) extends ConsistentHashable {
  override def consistentHashKey: Any = key
}

class Cache extends Actor {
  var cache = Map.empty[String, String]

  def receive = {
    case Entry(key, value) => cache += (key -> value)
    case Get(key) => sender() ! cache.get(key)
    case Evict(key) => cache -= key
  }
}






final case class Entry(key: String, value: String)

import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRoutingLogic

trait Work {}

class Master extends Actor {

  var router = {
    val routees = Vector.fill(5) {
      val r = context.actorOf(Props[Worker])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case w: Work =>
      router.route(w, sender())
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[Worker])
      context watch r
      router = router.addRoutee(r)
  }
}

class HelloWorld extends Actor {

  override def preStart(): Unit = {
    val greeter = context.actorOf(Props[Worker], "greeter")
    greeter ! Worker.Greet
  }

  override def receive: Receive = {
    case Worker.Done => context stop self
  }

}

object Worker {

  case object Done {}

  case object Greet {}

}

class Worker extends Actor {
  override def receive: Actor.Receive = {
    case Worker.Greet => println("yesYes")
    case _: Work => println("workWork")

  }
}