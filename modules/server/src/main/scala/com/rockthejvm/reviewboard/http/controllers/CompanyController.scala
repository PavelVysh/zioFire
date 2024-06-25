package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.*

import collection.mutable

class CompanyController private extends CompanyEndpoints with BaseController {

  val db = mutable.Map[Long, Company]()

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    ZIO.succeed {
      val id = db.size + 1
      val company = req.toCompany(id)
      db += (id.toLong -> company)
      company
    }
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess { _ =>
    ZIO.succeed(db.values.toList)
  }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { req =>
    ZIO.attempt(req.toLong).map(db.get)
  }
  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = ZIO.succeed(new CompanyController())
}
