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
package com.waz.model

import java.util.Date

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.waz.api.ErrorType
import com.waz.api.impl.ErrorResponse
import com.waz.db.Col._
import com.waz.db.Dao

case class ErrorData(id: Uid,
                     errType: ErrorType,
                     users: Seq[UserId] = Nil,
                     messages: Seq[MessageId] = Nil,
                     convId: Option[ConvId] = None,
                     responseCode: Int = 0,
                     responseMessage: String = "",
                     responseLabel: String = "",
                     time: Date = new Date) {
}

object ErrorData {

  def apply(errType: ErrorType, resp: ErrorResponse): ErrorData =
    new ErrorData(Uid(), errType, responseCode = resp.code, responseMessage = resp.message, responseLabel = resp.label)

  def apply(errType: ErrorType, resp: ErrorResponse, convId: ConvId): ErrorData =
    new ErrorData(Uid(), errType, convId = Some(convId), responseCode = resp.code, responseMessage = resp.message, responseLabel = resp.label)

  def apply(errType: ErrorType, resp: ErrorResponse, convId: ConvId, users: Seq[UserId]): ErrorData =
    new ErrorData(Uid(), errType, convId = Some(convId), users = users, responseCode = resp.code, responseMessage = resp.message, responseLabel = resp.label)

  implicit object ErrorDataDao extends Dao[ErrorData, Uid] {
    val Id = uid('_id, "PRIMARY KEY")(_.id)
    val Type = text[ErrorType]('err_type, _.name(), ErrorType.valueOf)(_.errType)
    val Users = text[Seq[UserId]]('users, _.mkString(","), { str => if(str.isEmpty) Nil else str.split(',').map(str => UserId(str)) })(_.users)
    val Messages = text[Seq[MessageId]]('messages, _.mkString(","), { str => if(str.isEmpty) Nil else str.split(',').map(str => MessageId(str)) })(_.messages)
    val ConvId = opt(id[ConvId]('conv_id))(_.convId)
    val ResCode = int('res_code)(_.responseCode)
    val ResMessage = text('res_msg)(_.responseMessage)
    val ResLabel = text('res_label)(_.responseLabel)
    val Time = date('time)(_.time)

    override val idCol = Id

    override val table = Table("Errors", Id, Type, Users, Messages, ConvId, ResCode, ResMessage, ResLabel, Time)

    override def apply(implicit cursor: Cursor): ErrorData = ErrorData(Id, Type, Users, Messages, ConvId, ResCode, ResMessage, ResLabel, Time)

    def listErrors(implicit db: SQLiteDatabase) = list(db.query(table.name, null, null, null, null, null, Time.name))
  }
}
