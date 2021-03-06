/*
 * Copyright 2018-2020 Radicalbit S.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.radicalbit.nsdb.cluster.logic

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.cluster.ddata.Replicator.{WriteAll, WriteConsistency, WriteLocal, WriteMajority}

import scala.concurrent.duration.FiniteDuration

trait WriteConfig { this: Actor =>
  private val config = context.system.settings.config

  private lazy val timeout: FiniteDuration =
    FiniteDuration(config.getDuration("nsdb.global.timeout", TimeUnit.SECONDS), TimeUnit.SECONDS)

  /**
    * Provides a Write Consistency policy from a config value.
    */
  protected lazy val metadataWriteConsistency: WriteConsistency = {
    val configValue = config.getString("nsdb.cluster.metadata-write-consistency")

    configValue.toLowerCase match {
      case "all"      => WriteAll(timeout)
      case "majority" => WriteMajority(timeout)
      case "local"    => WriteLocal
      case wrongConfigValue =>
        throw new IllegalArgumentException(s"$wrongConfigValue is not a valid value for metadata-write-consistency")
    }
  }

  /**
    * Parallel write-processing guarantees higher throughput while serial write-processing preserves the order of the operations.
    */
  sealed trait WriteProcessing
  case object Parallel extends WriteProcessing
  case object Serial   extends WriteProcessing

  protected lazy val writeProcessing: WriteProcessing = {
    val configValue = config.getString("nsdb.cluster.write-processing")

    configValue.toLowerCase match {
      case "parallel" => Parallel
      case "serial"   => Serial
      case wrongConfigValue =>
        throw new IllegalArgumentException(s"$wrongConfigValue is not a valid value for write-processing")
    }
  }
}
