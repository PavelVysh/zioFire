package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.services.CompanyService
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.test.ZIOSpecDefault
import zio.*
import zio.test.*
import sttp.client3.*
import zio.json.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import com.rockthejvm.reviewboard.syntax.*
object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val rtjvm = Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")
  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest) = ZIO.succeed(rtjvm)

    override def getById(id: Long) = ZIO.succeed {
      if (id == 1) Some(rtjvm)
      else None
    }

    override def getBySlug(slug: String) = ZIO.succeed {
      if (slug == rtjvm.slug) Some(rtjvm)
      else None
    }

    override def getAll = ZIO.succeed(List(rtjvm))
  }
  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    controllerZIO <- CompanyController.makeZIO
    backendStub <- ZIO.succeed(TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
      .whenServerEndpointRunLogic(endpointFun(controllerZIO))
      .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyControllerSpec")(
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson)
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption.flatMap(_.fromJson[Company].toOption)
            .contains(Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
        }
      },
      test("getAll") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body
        program.assert { respBody =>
          respBody.toOption.flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(rtjvm))
        }
      },
      test("getById") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption.flatMap(_.fromJson[Company].toOption).contains(rtjvm)
        }
      }
    ).provide(ZLayer.succeed(serviceStub))
}
