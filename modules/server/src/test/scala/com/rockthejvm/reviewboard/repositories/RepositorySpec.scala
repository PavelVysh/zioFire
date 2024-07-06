package com.rockthejvm.reviewboard.repositories

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.*

import javax.sql.DataSource

trait RepositorySpec {
  
  val initScript:String

  private def createContainer() = {
    java.lang.System.setProperty("DOCKER_HOST", "unix:///Users/pavlovy/.lima/docker.sock")
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript(initScript)
    container.start()
    container
  }

  private def createDatasource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(container.getJdbcUrl)
    dataSource.setUser(container.getUsername)
    dataSource.setPassword(container.getPassword)
    dataSource
  }

  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      datasource <- ZIO.attempt(createDatasource(container))
    } yield datasource
  }
}
