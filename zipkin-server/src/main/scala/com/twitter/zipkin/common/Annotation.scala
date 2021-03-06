/*
 * Copyright 2012 Twitter Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.twitter.zipkin.common

import com.twitter.zipkin.gen

/**
 * An annotation is a point in time with a string describing
 * what happened. For example one of the expected framework
 * generated annotations is "client send", when the client
 * initiated the request.
 */
object Annotation {
  val CoreClient = Seq(gen.Constants.CLIENT_SEND, gen.Constants.CLIENT_RECV)
  val CoreServer = Seq(gen.Constants.SERVER_SEND, gen.Constants.SERVER_RECV)

  // these annotations should always be present in a fully formed span
  val CoreAnnotations: Seq[String] = CoreClient ++ CoreServer

  def fromThrift(annotation: gen.Annotation): Annotation = {

    if (annotation.timestamp <= 0)
      throw new IllegalArgumentException("Annotation must have a timestamp: %s".format(annotation.toString))

    if ("".equals(annotation.value))
      throw new IllegalArgumentException("Annotation must have a value: %s".format(annotation.toString))

    new Annotation(annotation.timestamp, annotation.value,
      annotation.host.map(Endpoint.fromThrift(_)))
  }

}

/**
 * @param timestamp when was this annotation created? microseconds from epoch
 * @param value description of what happened at the timestamp could for example be "cache miss for key: x"
 * @param host host this annotation was created on
 */
case class Annotation(timestamp: Long, value: String, host: Option[Endpoint])
  extends Ordered[Annotation]{
  def serviceName = host.map(_.serviceName).getOrElse("Unknown service name")

  def toThrift: gen.Annotation = {
    gen.Annotation(timestamp, value, host.map(_.toThrift))
  }

  /**
   * @return diff between timestamps of the two annotations.
   */
  def -(annotation: Annotation): Long = timestamp - annotation.timestamp

  override def compare(that: Annotation): Int = {
    if (this.timestamp != that.timestamp)
      (this.timestamp - that.timestamp).toInt
    else if (this.value != that.value)
      this.value compare that.value
    else if (this.host != that.host)
      this.host.getOrElse {return -1} compare that.host.getOrElse {return 1}
    else
      0
  }
}
