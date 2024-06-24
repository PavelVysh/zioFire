package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.controllers.HealthController
import zio.*
import zio.http.Server
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}

object Application extends ZIOAppDefault {

  val serverProgram = for {
    healthController <- HealthController.makeZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(healthController.health))
  } yield ()
// dummy 1
  override def run = serverProgram.provide(
    Server.default
  )
}
