package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import zio.*
import zio.test.*
import com.rockthejvm.reviewboard.syntax.*

object CompanyServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CompanyService]
  val stubRepoLayer = ZLayer.succeed(
    new CompanyRepository {
      val db = collection.mutable.Map[Long, Company]()
      override def create(company: Company) = ZIO.succeed {
        val nextId     = db.keys.maxOption.getOrElse(0L) + 1
        val newCompany = company.copy(id = nextId)
        db += (nextId -> newCompany)
        newCompany
      }

      override def update(id: Long, op: Company => Company) = ZIO.attempt {
        val company = db(id)
        db += (id -> op(company))
        company
      }

      override def delete(id: Long) = ZIO.attempt {
        val company = db(id)
        db -= id
        company
      }

      override def getById(id: Long) = ZIO.succeed(db.get(id))

      override def getBySlug(slug: String) = ZIO.succeed(db.values.find(_.slug == slug))

      override def get = ZIO.succeed(db.values.toList)
    }
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("CompanyServiceTest")(
    test("create") {
      val companyZIO = service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
      companyZIO.assert { company =>
        company.name == "Rock the JVM" &&
        company.url == "rockthejvm.com" &&
        company.slug == "rock-the-jvm"
      }
    },
    test("getById") {
      val program = for {
        company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
        companyOpt <- service(_.getById(company.id))
      } yield (company, companyOpt)

      program.assert {
        case (company, Some(companyOpt)) =>
          company.name == "Rock the JVM" &&
          company.url == "rockthejvm.com" &&
          company.slug == "rock-the-jvm" &&
          company == companyOpt
        case _ => false
      }
    },
    test("getBySlug") {
      val program = for {
        company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
        companyOpt <- service(_.getBySlug(company.slug))
      } yield (company, companyOpt)

      program.assert {
        case (company, Some(companyOpt)) =>
          company.name == "Rock the JVM" &&
          company.url == "rockthejvm.com" &&
          company.slug == "rock-the-jvm" &&
          company == companyOpt
        case _ => false
      }
    },
    test("getAll") {
      val program = for {
        company   <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
        company2  <- service(_.create(CreateCompanyRequest("Google", "google.com")))
        companies <- service(_.getAll)
      } yield (company, company2, companies)

      program.assert { case (company, company2, companies) =>
        companies.toSet == Set(company, company2)
      }
    }
  ).provide(CompanyServiceLive.layer, stubRepoLayer)
}
