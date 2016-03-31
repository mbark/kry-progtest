package se.mbark.kry;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import javax.json.JsonReader;
import java.io.*;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends AbstractVerticle {
    private File dbFile;

    @Override
    public void start(Future<Void> fut) {
        startWebApp((http) -> completeStartup(http, fut));
    }

    public void stop(Future<Void> fut) {
        dbFile.delete();
        fut.complete();
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
        List<Service> services = new ArrayList<Service>();

        // reading from file to populate services

        HashMap<String, List<Service>> jsonMap = new HashMap<>();
        jsonMap.put("services", services);

        context.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(jsonMap));
    }

    private void addOne(RoutingContext context) {
        System.out.println(context.getBodyAsString());
        try {
            Service service = Json.decodeValue(context.getBodyAsString(), Service.class);
            context.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(service));
        } catch (DecodeException e) {
            System.out.println("Unable to decode context body: \"" + e.getMessage() + "\"");
            context.response()
                    .setStatusCode(400)
                    .end();
        }
    }

    private void deleteOne(RoutingContext context) {
       String id = context.request().getParam("id");
        if(id == null) {
            context.response().setStatusCode(400).end();
        } else {
            // pretend this is our service for now
            Service s = new Service();
            context.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(s));
        }
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        if (http.succeeded()) {
            try {
                dbFile = new File("resources/kry-db.txt");
                dbFile.delete();
                dbFile.mkdirs();
                dbFile.createNewFile();
            } catch (IOException e) {
                fut.fail("Unable to create file used to save service data");
            }
            fut.complete();
        } else {
            fut.fail(http.cause());
        }
    }
}


