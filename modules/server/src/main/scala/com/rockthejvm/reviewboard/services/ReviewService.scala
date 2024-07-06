package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import zio.*

import java.time.Instant

trait ReviewService {
  def create(req: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByUserId(id: Long): Task[List[Review]]
  def getByCompanyId(id: Long): Task[List[Review]]
  def delete(id: Long): Task[Review]
}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {

  override def create(req: CreateReviewRequest, userId: Long): Task[Review] = repo.create(
    Review(
      id = -1L,
      companyId = req.companyId,
      management = req.management,
      culture = req.culture,
      salary = req.salary,
      benefits = req.benefits,
      wouldRecommend = req.wouldRecommend,
      review = req.review,
      userId = userId,
      created = Instant.now(),
      updated = Instant.now()
    )
  )

  override def getById(id: Long): Task[Option[Review]] = repo.getById(id)

  override def getByUserId(id: Long): Task[List[Review]] = repo.getByUserId(id)

  override def getByCompanyId(id: Long): Task[List[Review]] = repo.getByCompanyId(id)

  override def delete(id: Long): Task[Review] = repo.delete(id)
}

object ReviewServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[ReviewRepository]
    } yield ReviewServiceLive(repo)
  }
}
