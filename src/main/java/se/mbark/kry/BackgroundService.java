package se.mbark.kry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BackgroundService extends AbstractVerticle {
    final Vertx vertx = Vertx.vertx();
    final FileSystem fileSystem = vertx.fileSystem();
    final HttpClient client = vertx.createHttpClient();
    String dbFilePath;

    @Override
    public void start(Future<Void> fut) {
        dbFilePath = config().getString("dbFile");
        vertx.setPeriodic(1000, request -> {
            pingAllServices();
        });
        fut.complete();
    }

    public void pingAllServices() {
        fileSystem.readFile(dbFilePath, read -> {
            if(read.succeeded()) {
                JsonObject jsonServices = new JsonObject(read.result().toString());
                JsonArray services = jsonServices.getJsonArray("services");
                for (int i = 0; i < services.size(); i++) {
                    JsonObject service = services.getJsonObject(i);
                    client.getAbs(service.getString("url"), response -> {
                        System.out.println("Pinging " + service.getString("url") + " got " + response.statusCode());
                    }).end();
                }

                Buffer b = Buffer.buffer();
                b.appendString(jsonServices.encodePrettily());

                fileSystem.writeFile(dbFilePath, b, write -> {
                });
            }
        });
    }

    public void stop(Future<Void> fut) {
        fut.complete();
    }

}
