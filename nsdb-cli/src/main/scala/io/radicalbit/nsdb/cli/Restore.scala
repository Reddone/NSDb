/*
 * Copyright 2018 Radicalbit S.r.l.
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

package io.radicalbit.nsdb.cli

import io.radicalbit.nsdb.client.rpc.GRPCClient
import io.radicalbit.nsdb.rpc.dump.{RestoreRequest, RestoreResponse}

import scala.concurrent.Await
import scala.util.{Failure, Success}

object Restore extends App {

  /**
    * Wrapper for Restore parameters.
    *
    * @param host rpc server address.
    * @param port rpc server port.
    * @param sourcePath source path containing the import bundle.
    */
  case class Params(host: Option[String] = None, port: Option[Int] = None, sourcePath: String)

  val parser = new scopt.OptionParser[Params]("scopt") {
    head("scopt", "3.x")
    opt[String]("host") action { (x, c) =>
      c.copy(host = Some(x))
    } text "the remote host"
    opt[Int]("port") action { (x, c) =>
      c.copy(port = Some(x))
    } text "the remote port"
    opt[String]("path").required() action { (x, c) =>
      c.copy(sourcePath = x)
    } text "path of the file to restore"
  }

  parser.parse(args, Params(None, None, "")) foreach { params =>
    import scala.concurrent.duration._

    val clientGrpc = new GRPCClient(host = params.host.getOrElse("127.0.0.1"), port = params.port.getOrElse(7817))

    Await.ready(clientGrpc.checkConnection(), 5.seconds).value.get match {
      case Success(_) =>
        val response: RestoreResponse = Await.result(clientGrpc.restore(RestoreRequest(params.sourcePath)), 10.seconds)
        if (response.startedSuccessfully) println("Restore process started successfully")
        else sys.error(response.errorMsg)
      case Failure(_) => sys.error(s"instance is not available at the moment.")
    }

  }

}
