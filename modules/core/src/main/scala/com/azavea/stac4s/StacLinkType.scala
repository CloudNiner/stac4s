package com.azavea.stac4s

import cats.Eq
import cats.implicits._
import io.circe._

sealed abstract class StacLinkType(val repr: String) {
  override def toString = repr
}

object StacLinkType {

  implicit def eqStacLinkType: Eq[StacLinkType] =
    Eq[String].imap(fromString)(_.repr)

  case object Self                                    extends StacLinkType("self")
  case object StacRoot                                extends StacLinkType("root")
  case object Parent                                  extends StacLinkType("parent")
  case object Child                                   extends StacLinkType("child")
  case object Item                                    extends StacLinkType("item")
  case object Items                                   extends StacLinkType("items")
  case object Source                                  extends StacLinkType("source")
  case object Collection                              extends StacLinkType("collection")
  case object License                                 extends StacLinkType("license")
  case object Alternate                               extends StacLinkType("alternate")
  case object DescribedBy                             extends StacLinkType("describedBy")
  case object Next                                    extends StacLinkType("next")
  case object Prev                                    extends StacLinkType("prev")
  case object ServiceDesc                             extends StacLinkType("service-desc")
  case object ServiceDoc                              extends StacLinkType("service-doc")
  case object Conformance                             extends StacLinkType("conformance")
  case object Data                                    extends StacLinkType("data")
  case object LatestVersion                           extends StacLinkType("latest-version")
  case object PredecessorVersion                      extends StacLinkType("predecessor-version")
  case object SuccessorVersion                        extends StacLinkType("successor-version")
  case object DerivedFrom                             extends StacLinkType("derived_from")
  final case class VendorLinkType(underlying: String) extends StacLinkType(underlying)

  private def fromString(s: String): StacLinkType = s.toLowerCase match {
    case "self"                => Self
    case "root"                => StacRoot
    case "parent"              => Parent
    case "child"               => Child
    case "item"                => Item
    case "items"               => Items
    case "alternate"           => Alternate
    case "collection"          => Collection
    case "describedby"         => DescribedBy
    case "next"                => Next
    case "license"             => License
    case "prev"                => Prev
    case "service-desc"        => ServiceDesc
    case "service-doc"         => ServiceDoc
    case "conformance"         => Conformance
    case "data"                => Data
    case "source"              => Source
    case "latest-version"      => LatestVersion
    case "predecessor-version" => PredecessorVersion
    case "successor-version"   => SuccessorVersion
    case "derived_from"        => DerivedFrom
    case _                     => VendorLinkType(s)
  }

  implicit val encStacLinkType: Encoder[StacLinkType] =
    Encoder.encodeString.contramap[StacLinkType](_.toString)

  implicit val decStacLinkType: Decoder[StacLinkType] =
    Decoder.decodeString.emap { str => Either.catchNonFatal(fromString(str)).leftMap(_ => "StacLinkType") }
}
