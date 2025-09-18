package com.rasel.client;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.rasel.common.ResponseParser;
import com.rasel.common.ResponseResource;

public class ResponseBus {
    private final Map<ResponseResource, CopyOnWriteArrayList<Consumer<ResponseParser>>> listeners = new ConcurrentHashMap<>();
    private final Map<ResponseResource, BlockingQueue<ResponseParser>> queues = new ConcurrentHashMap<>();
    private final Executor callbackExecutor;

    public ResponseBus() {
        // Use a small thread-pool to avoid running callbacks on the receiver thread
        this.callbackExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "resp-callback");
            t.setDaemon(true);
            return t;
        });
    }

    public AutoCloseable on(ResponseResource resource, Consumer<ResponseParser> handler) {
        listeners.computeIfAbsent(resource, k -> new CopyOnWriteArrayList<>()).add(handler);
        return () -> listeners.getOrDefault(resource, new CopyOnWriteArrayList<>()).remove(handler);
    }

    public BlockingQueue<ResponseParser> queue(ResponseResource resource) {
        return queues.computeIfAbsent(resource, k -> new LinkedBlockingQueue<>());
    }

    public void publish(ResponseParser resp) {
        // Ignore untagged responses (RESOURCE missing)
        final com.rasel.common.ResponseResource resource = resp != null ? resp.getResource() : null;
        if (resource == null) {
            return;
        }
        // Queue delivery
        queues.computeIfAbsent(resource, k -> new LinkedBlockingQueue<>()).offer(resp);
        // Callback delivery
        var ls = listeners.get(resource);
        if (ls != null) {
            for (var l : ls) {
                callbackExecutor.execute(() -> l.accept(resp));
            }
        }
    }
}