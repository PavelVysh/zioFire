package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import zio.*
import zio.test.*

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {
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
  val stubRepoLayer = ZLayer.succeed(
    new ReviewRepository {
      override def create(review: Review) = ZIO.succeed(goodReview)

      override def delete(id: Long) =
        getById(id).someOrFail(new RuntimeException(s"didnt get in test $id"))

      override def update(id: Long, op: Review => Review) =
        getById(id).someOrFail(new RuntimeException(s"didnt get in test $id")).map(op)

      override def getById(id: Long) = ZIO.succeed {
        id match
          case 1 => Some(goodReview)
          case 2 => Some(badReview)
          case _ => None
      }

      override def getByCompanyId(id: Long) =
        ZIO.succeed(
          if (id == 1) List(goodReview, badReview)
          else Nil
        )

      override def getByUserId(userId: Long) =
        ZIO.succeed(
          if (userId == 1) List(goodReview, badReview)
          else Nil
        )
    }
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("ReviewServiceTest")(
    test("create") {
      for {
        service <- ZIO.service[ReviewService]
        review <- service.create(
          CreateReviewRequest(
            companyId = goodReview.companyId,
            management = goodReview.management,
            culture = goodReview.culture,
            salary = goodReview.salary,
            benefits = goodReview.benefits,
            wouldRecommend = goodReview.wouldRecommend,
            review = goodReview.review
          ),
          goodReview.userId
        )
      } yield assertTrue(
        review.companyId == goodReview.companyId &&
          review.userId == goodReview.userId &&
          review.management == goodReview.management &&
          review.culture == goodReview.culture &&
          review.salary == goodReview.salary &&
          review.benefits == goodReview.benefits &&
          review.wouldRecommend == goodReview.wouldRecommend &&
          review.review == goodReview.review
      )
    },
    test("get by id") {
      for {
        service        <- ZIO.service[ReviewService]
        review         <- service.getById(1L)
        reviewNotFound <- service.getById(999L)
      } yield assertTrue {
        review.contains(goodReview) && reviewNotFound.isEmpty
      }
    },
    test("get by company") {
      for {
        service        <- ZIO.service[ReviewService]
        reviews        <- service.getByCompanyId(1)
        reviewNotFound <- service.getByCompanyId(999L)
      } yield assertTrue {
        reviews.toSet == Set(goodReview, badReview) && reviewNotFound.isEmpty
      }
    },
    test("get by useId") {
      for {
        service        <- ZIO.service[ReviewService]
        reviews        <- service.getByUserId(1)
        reviewNotFound <- service.getByUserId(999L)
      } yield assertTrue {
        reviews.toSet == Set(goodReview, badReview) && reviewNotFound.isEmpty
      }
    }
  ).provide(ReviewServiceLive.layer, stubRepoLayer)

}
