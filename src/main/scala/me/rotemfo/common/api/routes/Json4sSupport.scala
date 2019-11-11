package me.rotemfo.common.api.routes

import java.lang.reflect.InvocationTargetException

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentTypeRange
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Formats, MappingException, Serialization, native}

import scala.collection.immutable.Seq

/**
 * project: scala-demo-server
 * package: me.rotemfo.common.api.routes
 * file:    Json4sSupport
 * created: 2019-11-11
 * author:  rotem
 */
trait Json4sSupport {

  implicit val serialization: Serialization.type = native.Serialization
  implicit val formats: Formats = DefaultFormats

  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }
  private val jsonStringMarshaller = Marshaller.stringMarshaller(`application/json`)

  private def unmarshallerContentTypes: Seq[ContentTypeRange] =
    List(`application/json`)

  /**
   * HTTP entity => `A`
   *
   * @tparam A type to decode
   * @return unmarshaller for `A`
   */
  implicit def unmarshaller[A: Manifest](implicit serialization: Serialization,
                                         formats: Formats): FromEntityUnmarshaller[A] =
    jsonStringUnmarshaller
      .map(s => serialization.read(s))
      .recover { _ =>
        _ => {
          case MappingException(_, ite: InvocationTargetException) => throw ite.getCause
        }
      }

  /**
   * `A` => HTTP entity
   *
   * @tparam A type to encode, must be upper bounded by `AnyRef`
   * @return marshaller for any `A` value
   */

  import Json4sSupport.ShouldWritePretty

  implicit def marshaller[A <: AnyRef](implicit serialization: Serialization,
                                       formats: Formats,
                                       shouldWritePretty: ShouldWritePretty =
                                       ShouldWritePretty.False): ToEntityMarshaller[A] =
    shouldWritePretty match {
      case ShouldWritePretty.False =>
        jsonStringMarshaller.compose(serialization.write[A])
      case ShouldWritePretty.True =>
        jsonStringMarshaller.compose(serialization.writePretty[A])
    }
}

object Json4sSupport extends Json4sSupport {

  sealed abstract class ShouldWritePretty

  final object ShouldWritePretty {
    final object True extends ShouldWritePretty
    final object False extends ShouldWritePretty
  }
}
