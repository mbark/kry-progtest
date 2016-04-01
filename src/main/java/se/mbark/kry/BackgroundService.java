package se.mbark.kry;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.lang.rxjava.InternalHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BackgroundService extends AbstractVerticle {
    final Vertx vertx = Vertx.vertx();
    final HttpClient client = vertx.createHttpClient();
    DbFile dbFile;

    @Override
    public void start(Future<Void> fut) {
        dbFile = new DbFile(vertx.fileSystem());
        if(config().getBoolean("periodicPing")) {
            int period = config().getInteger("period");
            vertx.setPeriodic(period, request -> {
                pingAllServices(dbFile::getServices, ping -> {
                    if(ping.succeeded()) {
                        dbFile.updateServices(ping.result(), whatever -> { });
                    }
                });
            });
        }
        fut.complete();
    }

    // oh java, never change :>>>>>>>>
    public void pingAllServices(Consumer<Handler<AsyncResult<JsonObject>>> serviceSupplier, Handler<AsyncResult<JsonObject>> serviceConsumer) {
        serviceSupplier.accept(supply -> {
            if(supply.succeeded()) {
                JsonObject json = supply.result();
                JsonArray services = json.getJsonArray(DbFile.SERVICES_KEY);
                for (int i = 0; i < services.size(); i++) {
                    JsonObject service = services.getJsonObject(i);
                    client.getAbs(service.getString("url"), response -> {
                        System.out.println("Pinging " + service.getString("url") + " got " + response.statusCode());
                    }).end();
                }
                json.put(DbFile.SERVICES_KEY, services);

                serviceConsumer.handle(InternalHelper.result(json));
            } else {
                serviceConsumer.handle(InternalHelper.failure(supply.cause()));
            }
        });
    }

    public void stop(Future<Void> fut) {
        fut.complete();
    }

}
