/*
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.sync.client

import java.io._

import android.util.Base64
import com.waz.api.ProvisionedApiSpec
import com.waz.cache.LocalData
import com.waz.model.otr.ClientId
import com.waz.model.{AssetId, ConvId, ImageData, RImageDataId}
import com.waz.sync.client.ImageAssetClient.OtrAssetMetadata
import com.waz.sync.client.OtrClient.EncryptedContent
import com.waz.threading.Threading
import com.waz.utils.{IoUtils, returning}
import org.robolectric.annotation.Config
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

@Config(manifest = "tests/OtrAndroidManifest.xml")
class ImageAssetClientSpec extends FeatureSpec with Matchers with ProvisionedApiSpec with ScalaFutures {

  val provisionFile = "/one_conversation.json"

  implicit val timeout = 10.seconds: Timeout
  implicit lazy val ec = Threading.Background

  def client: ImageAssetClient = zmessaging.imageClient

  feature("sending") {

    scenario("post image asset data") {
      val imageData = IoUtils.toByteArray(getClass.getResourceAsStream("/images/penguin_128.png"))

      val conversations = api.getConversations
      withDelay(conversations should not be empty)

      val c = ConvId(conversations.get(0).getId)
      val assetId = AssetId()
      val data = new ImageData("tag", "image/png", 128, 128, 512, 512, imageData.length, Some(RImageDataId()), Some(Base64.encodeToString(imageData, Base64.NO_WRAP)))

      val response = for {
        conv <- zmessaging.convsStorage.get(c)
        res <- client.postImageAssetData(data, assetId, conv.get.remoteId, LocalData(imageData))
      } yield res

      val res = Await.result(response, 20.seconds)
      info(s"got response: $res")
      res should be('right)
      val Right(event) = res
      event.assetId shouldEqual assetId
    }

    scenario("post image asset") {
      val image = IoUtils.toByteArray(getClass.getResourceAsStream("/images/penguin.png"))

      val conversations = api.getConversations
      withDelay(conversations should not be empty)

      val c = conversations.get(0).asInstanceOf[com.waz.api.impl.Conversation].data.remoteId
      val input = api.ui.images.getOrCreateImageAssetFrom(image).asInstanceOf[com.waz.api.impl.LocalImageAsset]
      val response = for {
        asset <- zmessaging.assetGenerator.generateWireAsset(AssetId(), input.data.versions.last, c, profilePicture = false).future
        _ <- zmessaging.imageAssets.updateImageAssets(Seq(asset))
        conv <- zmessaging.convsStorage.getByRemoteId(c)
        res <- Future.sequence(asset.versions map { im =>
          awaitUi(1.second)
          zmessaging.cache.getEntry(im.cacheKey).future flatMap {
            case Some(entry) => client.postImageAssetData(im, asset.id, conv.get.remoteId, entry)
            case None => Future.successful(Left("meep"))
          }
        })
      } yield res

      val results = Await.result(response, 60.seconds)
      results should contain (Left("meep"))
      atLeast (1, results) should be('right)
    }

    scenario("post multiple assets") {
      val file = File.createTempFile("penguin", "png")
      IoUtils.copy(new ByteArrayInputStream(randomArray(14793)), file)

      val conversations = api.getConversations
      withDelay(conversations should not be empty)
      val c = conversations.get(0).asInstanceOf[com.waz.api.impl.Conversation].data.remoteId

      for (i <- 0 to 10) {
        withClue(i) {
          Await.result(client.postImageAssetData(ImageData("test", "image/png", 100, 100, 100, 100, file.length().toInt, Some(RImageDataId())), AssetId(), c, LocalData(file), nativePush = false), 5.seconds) shouldBe 'right
        }
      }
    }

    scenario("post otr asset") {
      val file = File.createTempFile("penguin", "png")
      IoUtils.copy(new ByteArrayInputStream(randomArray(14700)), file)

      val conversations = api.getConversations
      withDelay(conversations should not be empty)
      val c = conversations.get(0).asInstanceOf[com.waz.api.impl.Conversation].data.remoteId
      val clId = Await.result(zmessaging.otrClientsService.getSelfClient, 5.seconds).fold(ClientId())(_.id)

      for (i <- 0 to 10) {
        withClue(i) {
          Await.result(client.postOtrAsset(c, OtrAssetMetadata(clId, EncryptedContent(Map.empty), nativePush = false), LocalData(file), ignoreMissing = false), 5.seconds) shouldBe 'right
        }
      }
    }

    def randomArray(size: Int) = returning(new Array[Byte](size))(Random.nextBytes)
  }
}
