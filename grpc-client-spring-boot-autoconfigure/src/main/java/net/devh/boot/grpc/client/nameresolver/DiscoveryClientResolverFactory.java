/*
 * Copyright (c) 2016-2018 Michael Zhang <yidongnan@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.devh.boot.grpc.client.nameresolver;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.context.event.EventListener;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.internal.GrpcUtil;

/**
 * A name resolver factory that will create an {@link DiscoveryClientNameResolver} based on the target uri.
 *
 * @author Michael (yidongnan@gmail.com)
 * @since 5/17/16
 */
public class DiscoveryClientResolverFactory extends NameResolverProvider {

    private final Collection<DiscoveryClientNameResolver> discoveryClientNameResolvers = new ArrayList<>();
    private final HeartbeatMonitor monitor = new HeartbeatMonitor();

    private final DiscoveryClient client;

    public DiscoveryClientResolverFactory(final DiscoveryClient client) {
        this.client = client;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(final URI targetUri, final Attributes params) {
        final DiscoveryClientNameResolver discoveryClientNameResolver =
                new DiscoveryClientNameResolver(targetUri.toString(), this.client, GrpcUtil.TIMER_SERVICE,
                        GrpcUtil.SHARED_CHANNEL_EXECUTOR);
        this.discoveryClientNameResolvers.add(discoveryClientNameResolver);
        return discoveryClientNameResolver;
    }

    @Override
    public String getDefaultScheme() {
        return "discoveryClient";
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    /**
     * Triggers a refresh of the registered name resolvers.
     *
     * @param event The event that triggered the update.
     */
    @EventListener(HeartbeatEvent.class)
    public void heartbeat(final HeartbeatEvent event) {
        if (this.monitor.update(event.getValue())) {
            for (final DiscoveryClientNameResolver discoveryClientNameResolver : this.discoveryClientNameResolvers) {
                discoveryClientNameResolver.refresh();
            }
        }
    }

    /**
     * Cleans up the name resolvers.
     */
    @PreDestroy
    public void destroy() {
        this.discoveryClientNameResolvers.clear();
    }

}
