import java.net._
import scala.io.Source
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import org.jsoup.nodes.Document

import scala.concurrent._
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object crawler {
	case class ListApplication(name: String, url: String, rank: Int)
	case class Application(applicationInfo: ApplicationInfo)
	case class ApplicationInfo(name: String, description: String)

	def main(args: Array[String]) = {
		val categorySlugsBuffer = new scala.collection.mutable.ListBuffer[String]
		Source.fromFile("ios_ids.txt").getLines.foreach { line => categorySlugsBuffer += line }
		val categorySlugs = categorySlugsBuffer.toList

		for(categorySlug <- categorySlugs) {
			val doc = getDocFromUrl("https://itunes.apple.com/us/genre/" + categorySlug + "?mt=8")
			val apps = doc >> elements("#selectedgenre .column li")

			val appTasks = for((app, i) <- apps.zipWithIndex) yield Future { 
				crawlIndividualApp(ListApplication(app >> text("a"), app >> attr("href")("a"), i))
			}

			val aggregatedAppTasks = Future.sequence(appTasks)
			Await.result(aggregatedAppTasks, Duration.Inf)
		}
	}

	def crawlIndividualApp(app: ListApplication): Application = {
		println("Crawling " + app.name)
		val doc = getDocFromUrl(app.url)
		val applicationInfo = getApplicationsInfo(doc)
		println(s"Retrieved info: $applicationInfo")
		
		Application(applicationInfo)
	}

	def getApplicationsInfo(doc: Document): ApplicationInfo = {
		val name = doc >> text("h1")
		val langCode = "en"
		val description = doc >> text(".center-stack .product-review p")
		
		ApplicationInfo(name, description)
	}

	private def getDocFromUrl(url: String) = {
		val proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("52.69.225.107", 10020))
		val connection = new URL(url).openConnection(proxy)
		connection.setRequestProperty("Accept-Charset", "utf-8")
	
		val browser = new Browser
		browser.parseString(scala.io.Source.fromInputStream(connection.getInputStream).mkString)
	}
}
