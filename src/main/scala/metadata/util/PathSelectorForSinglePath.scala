package metadata.util

import csw.params.core.generics.KeyType.{IntKey, RaDecKey, StructKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.{RaDec, Struct}

import scala.util.chaining.scalaUtilChainingOps

object PathSelectorForSinglePath extends App {

  private val struct1Param: Parameter[_] = StructKey
    .make("struct1")
    .set(
      Struct().madd(
        StructKey
          .make("struct2")
          .setAll(
            Array(
              Struct().add(RaDecKey.make("radec3").set(RaDec(1, 243))),
              Struct().madd(RaDecKey.make("radec4").set(RaDec(1, 333)), IntKey.make("int").setAll(Array(12, 3, 4)))
            )
          ),
        StructKey
          .make("struct3")
          .setAll(
            Array(
              Struct().add(RaDecKey.make("radec6").set(RaDec(1, 234567), RaDec(ra = 1, dec = 999))),
              Struct().madd(RaDecKey.make("radec7").set(RaDec(1, 999)), IntKey.make("int").setAll(Array(12, 3, 4)))
            )
          )
      )
    )

  private val paramSet: Set[Parameter[_]] = Set(struct1Param)

  private val path = "struct1/struct3/radec6[1].dec".split("/").toList

  private val structPath: List[String] = path.dropRight(1)
  private val paramPath: String        = path.last

  val (paramKey, index, field) = {
    val (paramKeyAndIndex, x) = paramPath.splitAt(paramPath.indexOf("."))
    val field                 = x.drop(1)
    val (paramKey, rawIndex)  = paramKeyAndIndex.splitAt(paramKeyAndIndex.indexOf("["))
    val index                 = rawIndex.substring(1, rawIndex.length - 1).strip().toInt //remove [ and ] brackets

    println("paramKey " + paramKey)
    println("index " + index)
    println("field " + field)

    (paramKey, index, field)
  }

  def nest(path: List[String], paramSet: Set[Parameter[_]]): Set[Parameter[_]] =
    path match {
      case Nil => paramSet
      case head :: next =>
        paramSet.filter(_.keyName == head).flatMap(_.values.flatMap(x => nest(next, x.asInstanceOf[Struct].paramSet)))
    }

  private val lastParamSet = nest(structPath, paramSet)

  private val option: Option[AnyRef] = lastParamSet.find(_.keyName == paramKey).map { p =>
    val value: Any = p(index)

    val fieldField = value.getClass.getDeclaredField(field)
    fieldField.setAccessible(true)
    fieldField.get(value).tap(x => println("final value " + x))
  }
}
