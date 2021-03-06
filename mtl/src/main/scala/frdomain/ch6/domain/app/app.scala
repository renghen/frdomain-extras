package frdomain.ch6
package domain
package io
package app

import cats._
import cats.data._
import cats.implicits._
import cats.instances.all._
import cats.effect.IO
import cats.mtl._

import repository.AccountRepository
import repository.interpreter.AccountRepositoryInMemory

import service.interpreter._
import service.{ Checking, Savings }
import common._

object App {

  def main(args: Array[String]): Unit = {
    usecase1()
    usecase2()
    usecase3()
    usecase4()
  }

  def usecase1(): Unit = {

    import Implicits._
    val repo = new AccountRepositoryInMemory[IO]
    implicit val repositoryAsk = DefaultApplicativeAsk.constant[IO, AccountRepository[IO]](repo)
    val accountServiceIO = new AccountServiceInterpreter[IO]
    val reportingServiceIO = new ReportingServiceInterpreter[IO]

    import accountServiceIO._
    import reportingServiceIO._

    val opens = 
      for {
        _ <- open("a1234", "a1name", None, None, Checking)
        _ <- open("a2345", "a2name", None, None, Checking)
        _ <- open("a3456", "a3name", BigDecimal(5.8).some, None, Savings)
        _ <- open("a4567", "a4name", None, None, Checking)
        _ <- open("a5678", "a5name", BigDecimal(2.3).some, None, Savings)
      } yield (())
  
    val credits = 
      for {
        _ <- credit("a1234", 1000)
        _ <- credit("a2345", 2000)
        _ <- credit("a3456", 3000)
        _ <- credit("a4567", 4000)
      } yield (())
  
    val c = for {
      _ <- opens
      _ <- credits
      a <- balanceByAccount
    } yield a
  
    println(c.unsafeRunSync.toList)
  
    // (a2345,2000)
    // (a5678,0)
    // (a3456,3000)
    // (a1234,1000)
    // (a4567,4000)
  }

  def usecase2(): Unit = {

    import Implicits._
    val repo = new AccountRepositoryInMemory[IO]
    implicit val repositoryAsk = DefaultApplicativeAsk.constant[IO, AccountRepository[IO]](repo)
    val accountServiceIO = new AccountServiceInterpreter[IO]
    val reportingServiceIO = new ReportingServiceInterpreter[IO]

    import accountServiceIO._
    import reportingServiceIO._

    val c = for {
      _ <- open("a1234", "a1name", None, None, Checking)
      _ <- credit("a2345", 2000)
      a <- balanceByAccount
    } yield a

    c.unsafeRunAsync { 
      case Left(th) => th.printStackTrace
      case Right(vs) => vs.foreach(println)
    }

    // java.lang.Exception: Already existing account with no a1234
    //   at frdomain.ch6.domain.io.app.Implicits$$anon$1.raiseError(Implicits.scala:24)
    //   at frdomain.ch6.domain.io.app.Implicits$$anon$1.raiseError(Implicits.scala:18)
  }

  def usecase3(): Unit = {

    import Implicits._
    val repo = new AccountRepositoryInMemory[IO]
    implicit val repositoryAsk = DefaultApplicativeAsk.constant[IO, AccountRepository[IO]](repo)
    val accountServiceIO = new AccountServiceInterpreter[IO]
    val reportingServiceIO = new ReportingServiceInterpreter[IO]

    import accountServiceIO._
    import reportingServiceIO._

    val c = for {
      _ <- open("a1234", "a1name", None, None, Checking)
      _ <- credit("a1234", 2000)
      _ <- debit("a1234", 4000)
      a <- balanceByAccount
    } yield a

    c.unsafeRunAsync { 
      case Left(th) => th.printStackTrace
      case Right(vs) => vs.foreach(println)
    }

    // java.lang.Exception: Insufficient amount in a1234 to debit
    //   at frdomain.ch6.domain.io.app.Implicits$$anon$1.raiseError(Implicits.scala:24)
    //   at frdomain.ch6.domain.io.app.Implicits$$anon$1.raiseError(Implicits.scala:18)
  }

  def usecase4(): Unit = {

    import Implicits._
    val repo = new AccountRepositoryInMemory[IO]
    implicit val repositoryAsk = DefaultApplicativeAsk.constant[IO, AccountRepository[IO]](repo)
    val accountServiceIO = new AccountServiceInterpreter[IO]
    val reportingServiceIO = new ReportingServiceInterpreter[IO]

    import accountServiceIO._
    import reportingServiceIO._

    val c = for {
      a <- open("a134", "a1name", Some(BigDecimal(-0.9)), None, Savings)
      _ <- credit(a.no, 2000)
      _ <- debit(a.no, 4000)
      b <- balanceByAccount
    } yield b

    c.unsafeRunAsync { 
      case Left(th) => th.printStackTrace
      case Right(vs) => vs.foreach(println)
    }

    // java.lang.Exception: Account No has to be at least 5 characters long: found a134/Interest rate -0.9 must be > 0
    //   at frdomain.ch6.domain.io.app.Implicits$$anon$1.raiseError(Implicits.scala:24)
    //   at frdomain.ch6.domain.io.app.Implicits$$anon$1.raiseError(Implicits.scala:18)
  }
}
