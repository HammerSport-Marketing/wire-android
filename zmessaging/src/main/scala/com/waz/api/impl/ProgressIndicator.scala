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
package com.waz.api.impl

import com.waz.api.ProgressIndicator.State
import com.waz.api.impl.ProgressIndicator.ProgressData
import com.waz.threading.Threading
import com.waz.api

import scala.concurrent.Future

class ProgressIndicator extends api.ProgressIndicator with UiObservable {
  private var data = ProgressData(0L, -1L, State.RUNNING)

  def set(data: ProgressData): Unit = Future {
    this.data = data
    notifyChanged()
  } (Threading.Ui)

  def setState(state: State): Unit = Future {
    data = data.copy(state = state)
    notifyChanged()
  } (Threading.Ui)

  override def getProgress: Long = data.current

  override def getTotalSize: Long = data.total

  override def isIndefinite: Boolean = data.total == -1L

  override def getState: State = data.state

  override def toString: String = s"ProgressIndicator($data)"
}

object ProgressIndicator {
  type Callback = ProgressData => Unit

  case class ProgressData(current: Long, total: Long, state: State)
  object ProgressData {
    val Unknown = ProgressData(0, 0, State.UNKNOWN)
  }


  object Empty extends ProgressIndicator with UiObservable {
    override def getProgress: Long = 0L
    override def getTotalSize: Long = -1L
    override def isIndefinite = true
    override def getState = State.UNKNOWN
    override def set(data: ProgressData): Unit = ()
    override def setState(state: State): Unit = ()
  }
}
