package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest

import scala.collection.mutable
import zio.*

trait CompanyService {
  def create(req: CreateCompanyRequest): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def getAll: Task[List[Company]]
}

class CompanyServiceDummy extends CompanyService {
  val db = mutable.Map[Long, Company]()

  override def create(req: CreateCompanyRequest): Task[Company] = 
    ZIO.succeed {
    val id = db.size + 1
    val company = req.toCompany(id)
    db += (id.toLong -> company)
    company
  }

  override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(db.get(id))

  override def getAll: Task[List[Company]] = ZIO.succeed(db.values.toList)

  override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed(db.values.find(_.slug == slug))
}

object CompanyService {
  val dummyLayer = ZLayer.succeed(new CompanyServiceDummy)
}
