package se.mbark.kry;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.lang.rxjava.InternalHelper;

import java.util.Date;
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

    private void pingServicesRecursively(JsonArray services, int i, JsonArray updatedServices, Handler<JsonArray> result) {
        if(i >= services.size()) {
            result.handle(updatedServices);
            return;
        }

        JsonObject service = services.getJsonObject(i);
        client.getAbs(service.getString("url"), response -> {
            service.put("lastCheck", new Date().toString());
            service.put("status", response.statusCode() == 200 ? "OK" : "FAIL");
            updatedServices.add(service);
            pingServicesRecursively(services, i+1, updatedServices, result);
        }).end();
    }

    // oh java, never change :>>>>>>>>
    public void pingAllServices(Consumer<Handler<AsyncResult<JsonObject>>> serviceSupplier, Handler<AsyncResult<JsonObject>> serviceConsumer) {
        serviceSupplier.accept(supply -> {
            if(supply.succeeded()) {
                JsonObject json = supply.result();
                JsonArray services = json.getJsonArray(DbFile.SERVICES_KEY);

                pingServicesRecursively(services, 0, new JsonArray(), updatedServices -> {
                    json.put(DbFile.SERVICES_KEY, updatedServices);
                    serviceConsumer.handle(InternalHelper.result(json));
                });
            } else {
                serviceConsumer.handle(InternalHelper.failure(supply.cause()));
            }
        });
    }

    public void stop(Future<Void> fut) {
        fut.complete();
    }

}
