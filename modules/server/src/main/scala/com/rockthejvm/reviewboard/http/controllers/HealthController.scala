package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.*

class HealthController private extends HealthEndpoint with BaseController {
  val health: ServerEndpoint[Any, Task] = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("Health OK!"))
  
  override val routes: List[ServerEndpoint[Any, Task]] = List(health)
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}
