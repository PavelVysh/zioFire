package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.{BaseController, CompanyController, HealthController}

object HttpApi {
  def makeControllers = for {
    health <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
  } yield List(health, companies)

  def gatherRoutes(controllers: List[BaseController]) = controllers.flatMap(_.routes)
  
  val endpointsZIO = makeControllers.map(gatherRoutes)
}
