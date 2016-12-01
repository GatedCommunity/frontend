package common.Assets

import play.api.Mode.Dev
import common.{Logging, RelativePathEscaper}
import conf.Configuration
import org.apache.commons.io.IOUtils
import play.api.libs.json._
import play.api.Environment

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.util.{Failure, Success, Try}

// turns an unhashed name into a name that's hashed if it needs to be
class Assets(base: String, mapResource: String, useHashedBundles: Boolean = Configuration.assets.useHashedBundles) extends Logging {

  def lookup(implicit env: Environment): Map[String, String] = Get(assetMap(mapResource))

  def apply(path: String)(implicit env: Environment): String = {
    val target =
      if (useHashedBundles) {
        lookup.getOrElse(path, throw AssetNotFoundException(path))
      } else {
        path
      }
    base + target
  }

  def jsonToAssetMap(json: String): Try[Map[String, String]] =
    Json.parse(json).validate[Map[String, String]] match {
      case JsSuccess(m, _) => Success(m)
      case JsError(errors) => Failure(new Exception(s"$errors"))
    }

  def assetMap(resourceName: String)(implicit env: Environment): Try[Map[String, String]] = {
    for {
      rawResource <- LoadFromClasspath(resourceName)
      mappings <- jsonToAssetMap(rawResource)
    } yield mappings
  }

}

// turns a readable CSS class into a list of rules in short form from the atomic css file
class CssMap(mapResource: String)(implicit env: Environment) extends Logging {

  lazy val lookup: Map[String, List[String]] = Get(cssMap(mapResource))

  def apply(className: String): String = {
      className + ' ' + lookup.getOrElse(className, throw CssClassNotFoundException(className)).mkString(" ")
  }

  def jsonToAssetMap(json: String): Try[Map[String, List[String]]] =
    Json.parse(json).validate[Map[String, List[String]]] match {
      case JsSuccess(m, _) => Success(m)
      case JsError(errors) => Failure(new Exception(s"$errors"))
    }

  def cssMap(resourceName: String): Try[Map[String, List[String]]] = {
    for {
      rawResource <- LoadFromClasspath(resourceName)
      mappings <- jsonToAssetMap(rawResource)
    } yield mappings
  }

}

object inlineSvg {

  private val memoizedSvg: ConcurrentMap[String, Try[String]] = TrieMap()

  def apply(path: String)(implicit env: Environment): String =
    Get(memoizedSvg.getOrElseUpdate(path, LoadFromClasspath(s"assets/inline-svgs/$path")))

}

object css {

  private val memoizedCss: ConcurrentMap[String, Try[String]] = TrieMap()

  def head(projectOverride: Option[String])(implicit env: Environment) = inline(cssHead(projectOverride.getOrElse(Configuration.environment.projectName)))
  def inlineStoryPackage(implicit env: Environment) = inline("story-package")
  def atomic(implicit env: Environment) = inline("atomic")
  def inlineExplore(implicit env: Environment) = inline("article-explore")
  def amp(implicit env: Environment) = inline("head.amp")

  def projectCss(projectOverride: Option[String]) = project(projectOverride.getOrElse(Configuration.environment.projectName))
  def headOldIE(projectOverride: Option[String]) = cssOldIE(projectOverride.getOrElse(Configuration.environment.projectName))
  def headIE9(projectOverride: Option[String]) = cssIE9(projectOverride.getOrElse(Configuration.environment.projectName))

  private def inline(module: String)(implicit env: Environment): String = {
    val resourceName = s"assets/inline-stylesheets/$module.css"
    Get(if (env.mode == Dev) {
      LoadFromClasspath(resourceName)
    } else {
      memoizedCss.getOrElseUpdate(resourceName, LoadFromClasspath(resourceName))
    })
  }

  private def project(project: String): String = {
    project match {
      case "facia" => "stylesheets/facia.css"
      case _ => "stylesheets/content.css"
    }
  }

  private def cssHead(project: String): String =
    project match {
      case "footballSnaps" => "head.footballSnaps"
      case "facia" => "head.facia"
      case "identity" => "head.identity"
      case "football" => "head.football"
      case "index" => "head.index"
      case "rich-links" => "head.rich-links"
      case "email" => "head.email"
      case "commercial" => "head.commercial"
      case "survey" => "head.survey"
      case "signup" => "head.signup"
      case _ => "head.content"
    }

  private def cssOldIE(project: String): String =
    project match {
      case "facia" => "stylesheets/old-ie.head.facia.css"
      case "identity" => "stylesheets/old-ie.head.identity.css"
      case "football" => "stylesheets/old-ie.head.football.css"
      case "index" => "stylesheets/old-ie.head.index.css"
      case _ => "stylesheets/old-ie.head.content.css"
    }

  private def cssIE9(project: String): String =
    project match {
      case "facia" => "stylesheets/ie9.head.facia.css"
      case "identity" => "stylesheets/ie9.head.identity.css"
      case "football" => "stylesheets/ie9.head.football.css"
      case "index" => "stylesheets/ie9.head.index.css"
      case _ => "stylesheets/ie9.head.content.css"
    }

}

object js {
  def curl(implicit env: Environment): String = Get(LoadFromClasspath("assets/curl-domReady.js").map(RelativePathEscaper.escapeLeadingDotPaths))
}

object Get {
  def apply[T](`try`: Try[T]) = `try` match {
    case Success(s) => s
    case Failure(e) => throw e
  }
}

// gets the asset url from the classpath
object LoadFromClasspath {
  def apply(assetPath: String)(implicit env: Environment): Try[String] = {
    (Option(env.classLoader.getResource(assetPath)) match {
      case Some(s) => Success(s)
      case None => Failure(AssetNotFoundException(assetPath))
    }).flatMap { url =>
      Try(IOUtils.toString(url))
    }
  }
}

case class AssetNotFoundException(assetPath: String) extends Exception(s"Cannot find asset $assetPath. You should run `make compile`.")

case class CssClassNotFoundException(cssClass: String) extends Exception(s"Cannot find css class $cssClass in the atomic class mappings")
