package io.vertx.blog.first;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.File;
import java.rmi.server.UID;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a verticle. A verticle is a _Vert.x component_. This verticle is implemented in Java, but you can
 * implement them in JavaScript, Groovy or even Ruby.
 */
public class Server extends AbstractVerticle {
    private File dbFile;

    @Override
    public void start(Future<Void> fut) {
        dbFile = new File("kry-db.txt");
        startWebApp((http) -> completeStartup(http, fut));
    }

    public void stop(Future<Void> fut) {
        dbFile.delete();
        fut.complete();
    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        Router router = Router.router(vertx);

        router.route("/").handler(StaticHandler.create());
        router.get("/service").handler(routingContext -> {
            JsonObject services = getServices();

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(200)
                    .end(Json.encodePrettily(services));
        });

        router.post("/service").handler(routingContext -> {
            MultiMap params = routingContext.request().params();
            String name = params.get("name");
            String url = params.get("url");

            JsonObject addedService = addService(name, url);

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(201)
                    .end(Json.encodePrettily(addedService));
        });

        router.delete("/service/:serviceid").handler(routingContext -> {
            String serviceId = routingContext.request().getParam("serviceid");

            deleteService(serviceId);

            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(200)
                    .end();
        });

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080), next::handle);
    }

    private JsonObject getServices() {
        JsonObject json = new JsonObject();
        json.put("name", "google");
        json.put("url", "http://google.se");
        json.put("id", new UID().toString());

        return json;
    }

    private JsonObject addService (String name, String url) {
        JsonObject json = new JsonObject();
        json.put("name", name);
        json.put("url", url);
        json.put("id", new UID().toString());

        return json;
    }

    private JsonObject deleteService(String serviceId) {
        JsonObject json = new JsonObject();
        json.put("name", "google");
        json.put("url", "http://google.se");
        json.put("id", serviceId);

        return json;
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }
}


