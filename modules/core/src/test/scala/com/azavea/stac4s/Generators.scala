package com.azavea.stac4s

import com.azavea.stac4s.extensions.eo._
import com.azavea.stac4s.extensions.label._
import com.azavea.stac4s.extensions.label.LabelClassClasses._
import com.azavea.stac4s.extensions.layer.LayerItemExtension
import com.azavea.stac4s.extensions.asset._
import com.github.tbouron.SpdxLicense
import cats.data.NonEmptyList
import cats.implicits._
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.scalacheck.NumericInstances
import geotrellis.vector.{Geometry, Point, Polygon}
import io.circe.JsonObject
import io.circe.syntax._
import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.cats.implicits._
import java.time.Instant

object Generators extends NumericInstances {

  private def nonEmptyStringGen: Gen[String] =
    Gen.listOfN(30, Gen.alphaChar) map { _.mkString }

  private def nonEmptyAlphaRefinedStringGen: Gen[NonEmptyString] =
    nonEmptyStringGen map NonEmptyString.unsafeFrom

  private def nonEmptyListGen[T](g: Gen[T]): Gen[NonEmptyList[T]] =
    Gen.nonEmptyListOf(g) map { NonEmptyList.fromListUnsafe }

  private def rectangleGen: Gen[Geometry] =
    (for {
      lowerX <- Gen.choose(0.0, 1000.0)
      lowerY <- Gen.choose(0.0, 1000.0)
    } yield {
      Polygon(
        Point(lowerX, lowerY),
        Point(lowerX + 100, lowerY),
        Point(lowerX + 100, lowerY + 100),
        Point(lowerX, lowerY + 100),
        Point(lowerX, lowerY)
      )
    }).widen

  private def instantGen: Gen[Instant] = arbitrary[Int] map { x => Instant.now.plusMillis(x.toLong) }

  private def assetCollectionExtensionGen: Gen[AssetCollectionExtension] =
    Gen
      .mapOf(
        (nonEmptyStringGen, stacCollectionAssetGen).tupled
      )
      .map(AssetCollectionExtension.apply)

  private def collectionExtensionFieldsGen: Gen[JsonObject] = Gen.oneOf(
    Gen.const(().asJsonObject),
    assetCollectionExtensionGen.map(_.asJsonObject)
  )

  private def itemExtensionFieldsGen: Gen[JsonObject] = Gen.oneOf(
    Gen.const(().asJsonObject),
    labelExtensionPropertiesGen map { _.asJsonObject },
    layerPropertiesGen map { _.asJsonObject },
    eoItemExtensionGen map { _.asJsonObject }
  )

  private def labelLinkExtensionGen: Gen[LabelLinkExtension] =
    nonEmptyListGen(nonEmptyStringGen).map(LabelLinkExtension.apply)

  private def linkExtensionFields: Gen[JsonObject] = Gen.oneOf(
    Gen.const(().asJsonObject),
    labelLinkExtensionGen.map(_.asJsonObject)
  )

  private def eoAssetExtensionGen: Gen[EOAssetExtension] =
    nonEmptyListGen(arbitrary[BandRange]).map(EOAssetExtension.apply)

  private def assetExtensionFieldsGen: Gen[JsonObject] = Gen.oneOf(
    Gen.const(().asJsonObject),
    eoAssetExtensionGen.map(_.asJsonObject)
  )

  private def mediaTypeGen: Gen[StacMediaType] = Gen.oneOf(
    Gen.const(`image/tiff`),
    Gen.const(`image/vnd.stac.geotiff`),
    Gen.const(`image/cog`),
    Gen.const(`image/jp2`),
    Gen.const(`image/png`),
    Gen.const(`image/jpeg`),
    Gen.const(`text/xml`),
    Gen.const(`text/html`),
    Gen.const(`application/xml`),
    Gen.const(`application/json`),
    Gen.const(`text/plain`),
    Gen.const(`application/geo+json`),
    Gen.const(`application/geopackage+sqlite3`),
    Gen.const(`application/x-hdf5`),
    Gen.const(`application/x-hdf`),
    nonEmptyStringGen map VendorMediaType.apply
  )

  private def linkTypeGen: Gen[StacLinkType] = Gen.oneOf(
    Gen.const(StacLinkType.Self),
    Gen.const(StacLinkType.StacRoot),
    Gen.const(StacLinkType.Parent),
    Gen.const(StacLinkType.Child),
    Gen.const(StacLinkType.Item),
    Gen.const(StacLinkType.Items),
    Gen.const(StacLinkType.Source),
    Gen.const(StacLinkType.Collection),
    Gen.const(StacLinkType.License),
    Gen.const(StacLinkType.Alternate),
    Gen.const(StacLinkType.DescribedBy),
    Gen.const(StacLinkType.Next),
    Gen.const(StacLinkType.Prev),
    Gen.const(StacLinkType.ServiceDesc),
    Gen.const(StacLinkType.ServiceDoc),
    Gen.const(StacLinkType.Conformance),
    Gen.const(StacLinkType.Data),
    Gen.const(StacLinkType.LatestVersion),
    Gen.const(StacLinkType.PredecessorVersion),
    Gen.const(StacLinkType.SuccessorVersion),
    Gen.const(StacLinkType.DerivedFrom),
    nonEmptyStringGen map StacLinkType.VendorLinkType.apply
  )

  private def assetRoleGen: Gen[StacAssetRole] = Gen.oneOf(
    Gen.const(StacAssetRole.Thumbnail),
    Gen.const(StacAssetRole.Overview),
    Gen.const(StacAssetRole.Data),
    Gen.const(StacAssetRole.Metadata),
    nonEmptyStringGen map StacAssetRole.VendorAsset.apply
  )

  private def providerRoleGen: Gen[StacProviderRole] = Gen.oneOf(
    Licensor,
    Producer,
    Processor,
    Host
  )

  private def finiteDoubleGen: Gen[Double] =
    arbitrary[Double].filter(java.lang.Double.isFinite)

  private def twoDimBboxGen: Gen[TwoDimBbox] =
    (finiteDoubleGen, finiteDoubleGen, finiteDoubleGen, finiteDoubleGen)
      .mapN(TwoDimBbox.apply)

  private def spdxGen: Gen[SPDX] =
    arbitrary[SpdxLicense] map (license => SPDX(SpdxId.unsafeFrom(license.id)))

  private def proprietaryGen: Gen[Proprietary] = Gen.const(Proprietary())

  private def stacLicenseGen: Gen[StacLicense] = Gen.oneOf(spdxGen, proprietaryGen)

  private def threeDimBboxGen: Gen[ThreeDimBbox] =
    (
      finiteDoubleGen,
      finiteDoubleGen,
      finiteDoubleGen,
      finiteDoubleGen,
      finiteDoubleGen,
      finiteDoubleGen
    ).mapN(ThreeDimBbox.apply)

  private def bboxGen: Gen[Bbox] =
    Gen.oneOf(twoDimBboxGen.widen, threeDimBboxGen.widen)

  private def stacLinkGen: Gen[StacLink] =
    (
      nonEmptyStringGen,
      Gen.const(StacLinkType.Self), // self link type is required by TMS reification
      Gen.option(mediaTypeGen),
      Gen.option(nonEmptyStringGen),
      linkExtensionFields
    ).mapN(StacLink.apply)

  private def temporalExtentGen: Gen[TemporalExtent] = {
    (arbitrary[Instant], arbitrary[Instant]).tupled
      .map {
        case (start, end) =>
          TemporalExtent(start, end)
      }
  }

  private def stacExtentGen: Gen[StacExtent] =
    (
      bboxGen,
      temporalExtentGen
    ).mapN((bbox: Bbox, interval: TemporalExtent) => StacExtent(SpatialExtent(List(bbox)), Interval(List(interval))))

  private def stacProviderGen: Gen[StacProvider] =
    (
      nonEmptyStringGen,
      Gen.option(nonEmptyStringGen),
      Gen.listOf(providerRoleGen),
      Gen.option(nonEmptyStringGen)
    ).mapN(StacProvider.apply)

  private def stacItemAssetGen: Gen[StacItemAsset] =
    (
      nonEmptyStringGen,
      Gen.option(nonEmptyStringGen),
      Gen.option(nonEmptyStringGen),
      Gen.containerOf[Set, StacAssetRole](assetRoleGen),
      Gen.option(mediaTypeGen),
      assetExtensionFieldsGen
    ) mapN {
      StacItemAsset.apply
    }

  private def stacCollectionAssetGen: Gen[StacCollectionAsset] =
    (
      nonEmptyStringGen,
      Gen.option(nonEmptyStringGen),
      Gen.containerOf[Set, StacAssetRole](assetRoleGen) map { _.toList },
      mediaTypeGen
    ).mapN {
      StacCollectionAsset.apply
    }

  // Only do COGs for now, since we don't handle anything else in the example server.
  // As more types of stac items are supported, relax this assumption
  private def cogAssetGen: Gen[StacItemAsset] =
    stacItemAssetGen map { asset => asset.copy(_type = Some(`image/cog`)) }

  private def stacItemGen: Gen[StacItem] =
    (
      nonEmptyStringGen,
      Gen.const("0.8.0"),
      Gen.const(List.empty[String]),
      Gen.const("Feature"),
      rectangleGen,
      twoDimBboxGen,
      Gen.nonEmptyListOf(stacLinkGen),
      Gen.nonEmptyMap((nonEmptyStringGen, cogAssetGen).tupled),
      Gen.option(nonEmptyStringGen),
      itemExtensionFieldsGen
    ).mapN(StacItem.apply)

  private def stacCatalogGen: Gen[StacCatalog] =
    (
      nonEmptyStringGen,
      Gen.listOf(nonEmptyStringGen),
      nonEmptyStringGen,
      Gen.option(nonEmptyStringGen),
      nonEmptyStringGen,
      Gen.listOf(stacLinkGen),
      Gen.const(().asJsonObject)
    ).mapN(StacCatalog.apply)

  private def stacCollectionGen: Gen[StacCollection] =
    (
      nonEmptyStringGen,
      Gen.listOf(nonEmptyStringGen),
      nonEmptyStringGen,
      Gen.option(nonEmptyStringGen),
      nonEmptyStringGen,
      Gen.listOf(nonEmptyStringGen),
      stacLicenseGen,
      Gen.listOf(stacProviderGen),
      stacExtentGen,
      Gen.const(().asJsonObject),
      Gen.const(JsonObject.fromMap(Map.empty)),
      Gen.listOf(stacLinkGen),
      collectionExtensionFieldsGen
    ).mapN(StacCollection.apply)

  private def itemCollectionGen: Gen[ItemCollection] =
    (
      Gen.const("FeatureCollection"),
      Gen.const(StacVersion.unsafeFrom("0.9.0")),
      Gen.const(Nil),
      Gen.listOf[StacItem](stacItemGen),
      Gen.listOf[StacLink](stacLinkGen),
      Gen.const(().asJsonObject)
    ).mapN(ItemCollection.apply)

  private def labelClassNameGen: Gen[LabelClassName] =
    Gen.option(nonEmptyStringGen) map {
      case Some(s) => LabelClassName.VectorName(s.toLowerCase)
      case None    => LabelClassName.Raster
    }

  private def namedLabelClassesGen: Gen[LabelClassClasses] =
    nonEmptyListGen(nonEmptyStringGen) map { NamedLabelClasses.apply }

  private def numberedLabelClassesGen: Gen[LabelClassClasses] =
    nonEmptyListGen(arbitrary[Int]) map { NumberedLabelClasses.apply }

  private def labelClassClassesGen: Gen[LabelClassClasses] =
    Gen.oneOf(
      namedLabelClassesGen,
      numberedLabelClassesGen
    )

  private def labelClassGen: Gen[LabelClass] =
    (
      labelClassNameGen,
      labelClassClassesGen
    ).mapN(LabelClass.apply)

  private def labelCountGen: Gen[LabelCount] =
    (
      nonEmptyStringGen,
      arbitrary[Int]
    ).mapN(LabelCount.apply)

  private def labelStatsGen: Gen[LabelStats] =
    (
      nonEmptyStringGen,
      finiteDoubleGen
    ).mapN(LabelStats.apply)

  private def labelOverviewWithCounts: Gen[LabelOverview] =
    (
      nonEmptyStringGen,
      Gen.listOf(labelCountGen)
    ).mapN((key: String, counts: List[LabelCount]) => LabelOverview(key, counts, Nil))

  private def labelOverviewWithStats: Gen[LabelOverview] =
    (
      nonEmptyStringGen,
      Gen.listOf(labelStatsGen)
    ).mapN((key: String, stats: List[LabelStats]) => LabelOverview(key, Nil, stats))

  private def labelOverviewGen: Gen[LabelOverview] = Gen.oneOf(
    labelOverviewWithCounts,
    labelOverviewWithStats
  )

  private def labelTaskGen: Gen[LabelTask] =
    Gen.oneOf(
      Gen.const(LabelTask.Classification),
      Gen.const(LabelTask.Detection),
      Gen.const(LabelTask.Regression),
      Gen.const(LabelTask.Segmentation),
      nonEmptyStringGen map LabelTask.VendorTask.apply
    )

  private def labelMethodGen: Gen[LabelMethod] = Gen.oneOf(
    Gen.oneOf(
      Gen.const(LabelMethod.Automatic),
      Gen.const(LabelMethod.Manual),
      nonEmptyStringGen map LabelMethod.VendorMethod.apply
    ),
    nonEmptyStringGen map LabelMethod.fromString
  )

  private def labelTypeGen: Gen[LabelType] = Gen.oneOf(
    LabelType.Vector,
    LabelType.Raster
  )

  private def labelPropertiesGen: Gen[LabelProperties] =
    Gen.option(Gen.listOf(nonEmptyStringGen)).map(LabelProperties.fromOption)

  private def layerPropertiesGen: Gen[LayerItemExtension] =
    nonEmptyListGen(nonEmptyAlphaRefinedStringGen) map { LayerItemExtension.apply }

  private def labelExtensionPropertiesGen: Gen[LabelItemExtension] =
    (
      labelPropertiesGen,
      Gen.listOf(labelClassGen),
      nonEmptyStringGen,
      labelTypeGen,
      Gen.listOf(labelTaskGen),
      Gen.listOf(labelMethodGen),
      Gen.listOf(labelOverviewGen)
    ).mapN(LabelItemExtension.apply)

  private def bandGen: Gen[Band] =
    (
      nonEmptyAlphaRefinedStringGen,
      Gen.option(nonEmptyAlphaRefinedStringGen),
      Gen.option(nonEmptyAlphaRefinedStringGen),
      Gen.option(arbitrary[PosInt]),
      Gen.option(arbitrary[PosInt])
    ).mapN(Band.apply)

  private def eoItemExtensionGen: Gen[EOItemExtension] =
    (
      arbitrary[Int] map { _.toDouble }, // to avoid non-finite doubles
      nonEmptyListGen(bandGen),
      Gen.option(arbitrary[Percentage])
    ).mapN(EOItemExtension.apply)

  implicit val arbMediaType: Arbitrary[StacMediaType] = Arbitrary {
    mediaTypeGen
  }

  implicit val arbLinkType: Arbitrary[StacLinkType] = Arbitrary { linkTypeGen }

  implicit val arbProviderRole: Arbitrary[StacProviderRole] = Arbitrary {
    providerRoleGen
  }

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary { instantGen }

  implicit val arbGeometry: Arbitrary[Geometry] = Arbitrary { rectangleGen }

  implicit val arbItemAsset: Arbitrary[StacItemAsset] = Arbitrary { stacItemAssetGen }

  implicit val arbCollectionAsset: Arbitrary[StacCollectionAsset] = Arbitrary { stacCollectionAssetGen }

  implicit val arbItem: Arbitrary[StacItem] = Arbitrary { stacItemGen }

  implicit val arbCatalog: Arbitrary[StacCatalog] = Arbitrary { stacCatalogGen }

  implicit val arbCollection: Arbitrary[StacCollection] = Arbitrary {
    stacCollectionGen
  }

  implicit val arbStacExtent: Arbitrary[StacExtent] = Arbitrary {
    stacExtentGen
  }

  implicit val arbTwoDimBbox: Arbitrary[TwoDimBbox] = Arbitrary {
    twoDimBboxGen
  }

  implicit val arbThreeDimBbox: Arbitrary[ThreeDimBbox] = Arbitrary {
    threeDimBboxGen
  }

  implicit val arbTemporalExtent: Arbitrary[TemporalExtent] = Arbitrary {
    temporalExtentGen
  }

  implicit val arbBbox: Arbitrary[Bbox] = Arbitrary {
    bboxGen
  }

  implicit val arbSPDX: Arbitrary[SPDX] = Arbitrary { spdxGen }

  implicit val arbItemCollection: Arbitrary[ItemCollection] = Arbitrary {
    itemCollectionGen
  }

  implicit val arbStacAssetRole: Arbitrary[StacAssetRole] = Arbitrary {
    assetRoleGen
  }

  implicit val arbStacLink: Arbitrary[StacLink] = Arbitrary {
    stacLinkGen
  }

  implicit val arbLabelClassName: Arbitrary[LabelClassName] = Arbitrary { labelClassNameGen }

  implicit val arbLabelClassClasses: Arbitrary[LabelClassClasses] = Arbitrary { labelClassClassesGen }

  implicit val arbLabelClass: Arbitrary[LabelClass] = Arbitrary { labelClassGen }

  implicit val arbLabelCount: Arbitrary[LabelCount] = Arbitrary { labelCountGen }

  implicit val arbLabelStats: Arbitrary[LabelStats] = Arbitrary { labelStatsGen }

  implicit val arbLabelOverview: Arbitrary[LabelOverview] = Arbitrary { labelOverviewGen }

  implicit val arbLabelTask: Arbitrary[LabelTask] = Arbitrary { labelTaskGen }

  implicit val arbLabelMethod: Arbitrary[LabelMethod] = Arbitrary { labelMethodGen }

  implicit val arbLabelType: Arbitrary[LabelType] = Arbitrary { labelTypeGen }

  implicit val arbLabelProperties: Arbitrary[LabelProperties] = Arbitrary { labelPropertiesGen }

  implicit val arbLabelExtensionProperties: Arbitrary[LabelItemExtension] = Arbitrary {
    labelExtensionPropertiesGen
  }

  implicit val arbLabelLinkExtension: Arbitrary[LabelLinkExtension] = Arbitrary {
    labelLinkExtensionGen
  }

  implicit val arbLayerProperties: Arbitrary[LayerItemExtension] = Arbitrary { layerPropertiesGen }

  implicit val arbAssetExtensionProperties: Arbitrary[AssetCollectionExtension] = Arbitrary {
    assetCollectionExtensionGen
  }

  implicit val arbEOItemExtension: Arbitrary[EOItemExtension] = Arbitrary {
    eoItemExtensionGen
  }

  implicit val arbBand: Arbitrary[Band] = Arbitrary {
    bandGen
  }

  implicit val arbEOAssetExtension: Arbitrary[EOAssetExtension] = Arbitrary {
    eoAssetExtensionGen
  }
}
