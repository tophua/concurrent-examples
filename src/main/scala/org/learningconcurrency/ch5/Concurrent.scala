package ch5

import org.learningconcurrency._
import ch5._
import ParHtmlSpecSearch._
import scala.io.Source
import scala.concurrent.Future



/**
 * 并发不正常
 */
object ConcurrentWrong extends App {
  import scala.collection._
  import scala.concurrent.ExecutionContext.Implicits.global
  //注意返回数据类型
  def getUrlSpec(): Future[Seq[String]] = Future {
    val f = Source.fromURL("http://www.w3.org/Addressing/URL/url-spec.txt")
    try{
      f.getLines.toList 
    }finally{
      f.close()
    }
  }
  def intersection(a: GenSet[String], b: GenSet[String]): GenSet[String] = {
    //可变HashSet,不支持并发
    val result = new mutable.HashSet[String]
    //set迭代方式
    for (x <- a.par) if (b contains x) result.add(x)
    result
  }
  //推倒迭代方式,返回的数据类型Future[Seq[String]]
  val ifut = for {
    htmlSpec <- getHtmlSpec()
    urlSpec <- getUrlSpec()
  } yield {
    val htmlWords = htmlSpec.mkString.split("\\s+").toSet
    val urlWords = urlSpec.mkString.split("\\s+").toSet
    intersection(htmlWords, urlWords)
  }

  ifut onComplete {
    case t => log(s"Result: $t")
  }
  //注意不加入,不显示内容
  Thread.sleep(2500)
}
/**
 * 并发集合
 * 
 */

object ConcurrentCollections extends App {
  import java.util.concurrent.ConcurrentSkipListSet
  import scala.collection._
  import scala.collection.convert.decorateAsScala._
  import scala.concurrent.ExecutionContext.Implicits.global
  import ParHtmlSpecSearch.getHtmlSpec
 

  //流关闭
  def getUrlSpec(): Future[Seq[String]] = Future {
    val f = Source.fromURL("http://www.w3.org/Addressing/URL/url-spec.txt")
    try f.getLines.toList finally f.close()
  }
  
  def intersection(a: GenSet[String], b: GenSet[String]): GenSet[String] = {
    //可变ConcurrentSkipListSet,支持并发
    val skiplist = new ConcurrentSkipListSet[String]
    for (x <- a.par) if (b contains x) skiplist.add(x)
    val result: Set[String] = skiplist.asScala
    result
  }
  //注意类型推倒,
  val ifut = for {
    htmlSpec <- getHtmlSpec()
    urlSpec <- getUrlSpec()
  } yield {
    val htmlWords = htmlSpec.mkString.split("\\s+").toSet
    val urlWords = urlSpec.mkString.split("\\s+").toSet
    intersection(htmlWords, urlWords)
  }

  ifut foreach { case i =>
    log(s"intersection = $i")
  }
  //注意不加入,不显示内容
  Thread.sleep(20000)
}


object ConcurrentCollectionsBad extends App {
  import java.util.concurrent.ConcurrentSkipListSet
  import scala.collection._
  import scala.collection.parallel._

  def toPar[T](c: ConcurrentSkipListSet[T]): ParSet[T] = ???

  val c = new ConcurrentSkipListSet[Int]
  for (i <- 0 until 100) c.add(i)
  
  for (x <- toPar(c)) c.add(x) // bad
}


object ConcurrentTrieMap extends App {
  import scala.collection._
    //并发Map处理
  val cache = new concurrent.TrieMap[Int, String]()
  for (i <- 0 until 100) cache(i) = i.toString

  for ((number, string) <- cache.par) cache(-number) = s"-$string"

  log(s"cache - ${cache.keys.toList.sorted}")
}



