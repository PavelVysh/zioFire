package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.http.controllers.{CompanyController, HealthController}
import com.rockthejvm.reviewboard.repositories.{CompanyRepositoryLive, Repository}
import com.rockthejvm.reviewboard.services.{CompanyService, CompanyServiceLive}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*
import zio.http.Server
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints))
  } yield ()

  override def run = serverProgram.provide(
    Server.default,
    CompanyServiceLive.layer,
    CompanyRepositoryLive.layer,
    Repository.dataLayer
  )
}
