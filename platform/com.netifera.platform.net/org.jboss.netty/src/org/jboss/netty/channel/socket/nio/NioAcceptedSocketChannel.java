/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.channel.socket.nio;

import static org.jboss.netty.channel.Channels.*;

import java.nio.channels.SocketChannel;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;

/**
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 *
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 *
 */
final class NioAcceptedSocketChannel extends NioSocketChannel {

    final Thread bossThread;

    NioAcceptedSocketChannel(
            ChannelFactory factory, ChannelPipeline pipeline,
            Channel parent, ChannelSink sink,
            SocketChannel socket, NioWorker worker, Thread bossThread) {

        super(parent, factory, pipeline, sink, socket, worker);

        this.bossThread = bossThread;

        fireChannelOpen(this);
        fireChannelBound(this, getLocalAddress());
        fireChannelConnected(this, getRemoteAddress());
    }
}
