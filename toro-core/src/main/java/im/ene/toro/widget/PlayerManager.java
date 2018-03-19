/*
 * Copyright (c) 2017 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.ene.toro.widget;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.RecyclerView;
import im.ene.toro.PlayerDispatcher;
import im.ene.toro.ToroPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manage the collection of {@link ToroPlayer}s for a specific {@link Container}.
 *
 * Task: collect all Players in which "{@link Common#allowsToPlay(ToroPlayer)}" returns true, then
 * initialize them.
 *
 * @author eneim | 5/31/17.
 */
@SuppressWarnings({ "unused", "UnusedReturnValue", "StatementWithEmptyBody" }) //
final class PlayerManager {

  private final Container container;

  PlayerManager(Container container) {
    this.container = container;
  }

  // Make sure each ToroPlayer will present only once in this Manager.
  private final Set<ToroPlayer> players = new ArraySet<>();

  boolean attachPlayer(@NonNull ToroPlayer player) {
    return players.add(player);
  }

  boolean detachPlayer(@NonNull ToroPlayer player) {
    return players.remove(player);
  }

  boolean manages(@NonNull ToroPlayer player) {
    return players.contains(player);
  }

  /**
   * Return a "Copy" of the collection of players this manager is managing.
   *
   * @return a non null collection of Players those a managed.
   */
  @NonNull List<ToroPlayer> getPlayers() {
    return new ArrayList<>(this.players);
  }

  void initialize(@NonNull ToroPlayer player) {
    player.initialize(container, container.getPlaybackInfo(player.getPlayerOrder()));
  }

  void play(@NonNull ToroPlayer player) {
    int delay = container.playerDispatcher.getDelayToPlay(player);
    if (delay < PlayerDispatcher.DELAY_INFINITE) throw new IllegalArgumentException("Too negative");

    handler.removeMessages(MSG_PLAY, player); // remove unsent msg
    if (delay == PlayerDispatcher.DELAY_INFINITE) {
      // do nothing
    } else if (delay == PlayerDispatcher.DELAY_NONE) {
      player.play();
    } else {
      handler.sendMessageDelayed(handler.obtainMessage(MSG_PLAY, player), delay);
    }
  }

  void pause(@NonNull ToroPlayer player) {
    handler.removeCallbacksAndMessages(player); // remove all msg sent for the player
    player.pause();
  }

  // return false if this manager could not release the player.
  // normally when this manager doesn't manage the player.
  boolean release(@NonNull ToroPlayer player) {
    handler.removeCallbacksAndMessages(null);
    if (manages(player)) {
      player.release();
      return true;
    } else {
      return false;
    }
  }

  void recycle(ToroPlayer player) {
    // no-ops, place holder only.
    handler.removeCallbacksAndMessages(null);
  }

  void clear() {
    handler.removeCallbacksAndMessages(null);
    this.players.clear();
  }

  void onContainerScrollStateChanged(int state) {
    if (state != RecyclerView.SCROLL_STATE_IDLE) {
      handler.removeMessages(MSG_PLAY);
    }
  }

  @SuppressWarnings("WeakerAccess") static final int MSG_PLAY = 100;

  private final Handler handler = new Handler(Looper.getMainLooper()) {
    @Override public void handleMessage(Message msg) {
      if (!(msg.obj instanceof ToroPlayer)) return;
      if (msg.what == MSG_PLAY) {
        ToroPlayer player = (ToroPlayer) msg.obj;
        player.play();
      }
    }
  };
}
