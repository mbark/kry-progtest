package se.mbark.kry;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class Server extends AbstractVerticle {
    final Vertx vertx = Vertx.vertx();
    final FileSystem fileSystem = vertx.fileSystem();

    DbFile dbFile;

    @Override
    public void start(Future<Void> fut) {
        dbFile = new DbFile(fileSystem);

        JsonObject config = new JsonObject()
                .put("periodicPing", true)
                .put("period", 60000);

        DeploymentOptions options = new DeploymentOptions()
                .setWorker(true)
                .setConfig(config);
        vertx.deployVerticle("se.mbark.kry.BackgroundService", options);

        startWebApp((http) -> completeStartup(http, fut));
    }

    public void stop(Future<Void> fut) {
        dbFile.deleteYourself(fut);
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        Router router = Router.router(vertx);

        router.route("/").handler(StaticHandler.create());
        router.get("/service").handler(this::getAll);
        router.route("/service*").handler(BodyHandler.create());
        router.post("/service").handler(this::addOne);
        router.delete("/service/:id").handler(this::deleteOne);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080), next::handle);
    }

    private void getAll(RoutingContext context) {
        dbFile.getServices(get -> {
            if (get.succeeded()) {
                context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(get.result().encodePrettily());
            } else {
                context.response()
                        .setStatusCode(400);
            }
        });
    }

    private void addOne(RoutingContext context) {
        Service service = Json.decodeValue(context.getBodyAsString(), Service.class);
        dbFile.addService(service, add -> {
            if(add.succeeded()) {
                context.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(service));
            } else {
                context.response()
                        .setStatusCode(400)
                        .end();
            }
        });
    }

    private void deleteOne(RoutingContext context) {
        int id = -1;
        try {
            id = Integer.parseInt(context.request().getParam("id"));
        } catch(NumberFormatException e) {
            context.response().setStatusCode(400).end();
            return;
        }

        dbFile.deleteService(id, delete -> {
            if(delete.succeeded()) {
                context.response().setStatusCode(204).end();
            } else {
                context.response().setStatusCode(404).end();
            }
        });
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            dbFile.createYourself(fut);
        } else {
            fut.fail(http.cause());
        }
    }
}
