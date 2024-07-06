package com.rockthejvm.reviewboard.repositories

import org.testcontainers.containers.PostgreSQLContainer
import com.rockthejvm.reviewboard.domain.data.Company
import zio.*
import zio.test.*
import com.rockthejvm.reviewboard.syntax.*

import java.sql.SQLException
import javax.sql.DataSource

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  private val rtjvm = Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  override val initScript = "sql/companies.sql"

  private def genCompany(): Company =
    Company(-1L, genString(), genString(), genString())

  private def genString(): String = scala.util.Random.alphanumeric.take(8).mkString

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
        } yield company
        program.assert {
          case Company(_, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _) => true
          case _                                                                           => false
        }
      },
      test("creating duplicate should error") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          _    <- repo.create(rtjvm)
          err  <- repo.create(rtjvm).flip
        } yield err
        program.assert(_.isInstanceOf[SQLException])
      },
      test("get by id and slig") {
        val program = for {
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(rtjvm)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        } yield (company, fetchedById, fetchedBySlug)
        program.assert { case (company, fetchedById, fetchedBySlug) =>
          fetchedById.contains(company) && fetchedBySlug.contains(company)
        }
      },
      test("update record") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rtjvm)
          updated     <- repo.update(company.id, _.copy(url = "blog.rockthejvm.com"))
          fetchedById <- repo.getById(updated.id)
        } yield (updated, fetchedById)
        program.assert { case (updated, fetchedById) =>
          fetchedById.contains(updated)
        }
      },
      test("delete record") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rtjvm)
          _           <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        } yield fetchedById
        program.assert(_.isEmpty)
      },
      test("get all records") {
        val program = for {
          repo             <- ZIO.service[CompanyRepository]
          companies        <- ZIO.collectAll((1 to 10).map(_ => repo.create(genCompany())))
          companiesFetched <- repo.get
        } yield (companies, companiesFetched)
        program.assert { case (companies, companiesFetched) =>
          companies.toSet == companiesFetched.toSet
        }
      }
    ).provide(
      CompanyRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
