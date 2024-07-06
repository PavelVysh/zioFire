package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Review
import zio.test.*
import zio.*
import com.rockthejvm.reviewboard.syntax.*

import java.time.Instant
import java.util.concurrent.TimeUnit

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript = "sql/reviews.sql"

  val goodReview = Review(
    1L,
    1L,
    1L,
    5,
    5,
    5,
    5,
    10,
    "all good",
    Instant.now(),
    Instant.now()
  )

  val badReview = Review(
    2L,
    1L,
    1L,
    1,
    1,
    1,
    1,
    1,
    "all bad",
    Instant.now(),
    Instant.now()
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("ReviewRepositorySpec")(
    test("create review") {
      val program = for {
        repo   <- ZIO.service[ReviewRepository]
        review <- repo.create(goodReview)
      } yield review
      program.assert { review =>
        review.management == goodReview.management &&
        review.culture == goodReview.culture &&
        review.salary == goodReview.salary &&
        review.benefits == goodReview.benefits &&
        review.wouldRecommend == goodReview.wouldRecommend &&
        review.review == goodReview.review
      }
    },
    test("get review by ids (id, companyId, userId)") {
      for {
        repo              <- ZIO.service[ReviewRepository]
        review            <- repo.create(goodReview)
        reviewById        <- repo.getById(review.id)
        reviewByCompanyId <- repo.getByCompanyId(review.companyId)
        reviewByUserId    <- repo.getByUserId(review.userId)
      } yield assertTrue(
        reviewById.contains(review) &&
          reviewByUserId.contains(review) &&
          reviewByCompanyId.contains(review)
      )
    },
    test("get all") {
      for {
        repo          <- ZIO.service[ReviewRepository]
        review1       <- repo.create(goodReview)
        review2       <- repo.create(badReview)
        reviews       <- repo.getByCompanyId(review1.companyId)
        reviewsByUser <- repo.getByUserId(1L)
      } yield assertTrue(
        reviews.toSet == Set(review1, review2) &&
          reviewsByUser.toSet == Set(review1, review2)
      )
    },
    test("edit review") {
      for {
        repo    <- ZIO.service[ReviewRepository]
        review  <- repo.create(goodReview)
        updated <- repo.update(review.id, _.copy(review = "not too bad"))
      } yield assertTrue(
        review.id == updated.id &&
          review.companyId == updated.companyId &&
          review.userId == updated.userId &&
          review.management == updated.management &&
          review.culture == updated.culture &&
          review.salary == updated.salary &&
          review.benefits == updated.benefits &&
          review.wouldRecommend == updated.wouldRecommend &&
          updated.review == "not too bad" &&
          review.created == updated.created &&
          review.updated != updated.updated
      )
    },
    test("delete") {
      for {
        repo        <- ZIO.service[ReviewRepository]
        review      <- repo.create(goodReview)
        _           <- repo.delete(review.id)
        maybeReview <- repo.getById(review.id)
      } yield assertTrue(
        maybeReview.isEmpty
      )
    }
  ).provide(ReviewRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
