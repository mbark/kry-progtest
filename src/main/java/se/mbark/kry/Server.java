package se.mbark.kry;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class Server extends AbstractVerticle {
    Vertx vertx = Vertx.vertx();
    FileSystem fileSystem = vertx.fileSystem();
    String dbFilePath = "db.json";

    @Override
    public void start(Future<Void> fut) {
        startWebApp((http) -> completeStartup(http, fut));
    }

    public void stop(Future<Void> fut) {
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
        fileSystem.readFile(dbFilePath, result -> {
            if(result.succeeded()) {
                context.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(result.result());
            } else {
                context.response()
                        .setStatusCode(400);
            }
        });
    }

    private void addOne(RoutingContext context) {
        try {
            fileSystem.readFile(dbFilePath, read -> {
                if(read.succeeded()) {
                    Service service = Json.decodeValue(context.getBodyAsString(), Service.class);

                    JsonObject jsonServices = new JsonObject(read.result().toString());
                    JsonArray services =  jsonServices.getJsonArray("services");
                    services.add(service.toJson());
                    jsonServices.put("services", services);

                    Buffer b = Buffer.buffer();
                    b.appendString(jsonServices.encodePrettily());

                    fileSystem.writeFile(dbFilePath, b, write -> {
                        if(write.succeeded()) {
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
            });
        } catch (DecodeException e) {
            System.out.println("Unable to decode context body: \"" + e.getMessage() + "\"");
            context.response()
                    .setStatusCode(400)
                    .end();
        }
    }

    private void deleteOne(RoutingContext context) {
        fileSystem.readFile(dbFilePath, read -> {
            if(read.succeeded()) {
                JsonObject jsonServices = new JsonObject(read.result().toString());
                JsonArray services =  jsonServices.getJsonArray("services");

                int id = -1;
                try {
                    id = Integer.parseInt(context.request().getParam("id"));
                } catch(NumberFormatException e) {
                }

                if(id < 0) {
                    context.response().setStatusCode(400).end();
                }

                boolean deleted = false;

                for (int i = services.size() - 1; i >= 0; i--) {
                    JsonObject o = services.getJsonObject(i);
                    if(o.getInteger("id") == id) {
                        services.remove(i);
                        deleted = true;
                        break;
                    }
                }

                if(!deleted) {
                    context.response().setStatusCode(404).end();
                    return;
                }

               jsonServices.put("services", services);

                Buffer b = Buffer.buffer();
                b.appendString(jsonServices.encodePrettily());

                fileSystem.writeFile(dbFilePath, b, write -> {
                    if(write.succeeded()) {
                        context.response().setStatusCode(204).end();
                    } else {
                        context.response().setStatusCode(400).end();
                    }
                });
            }
        });
    }

    private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
        // callback hell
        if (http.succeeded()) {
            fileSystem.delete(dbFilePath, deleted -> {
                if(deleted.succeeded()) {
                    fileSystem.createFile(dbFilePath, created -> {
                        if(created.succeeded()) {
                            Buffer b  = Buffer.buffer();
                            b.appendString("{\"services\": [] }");
                            fileSystem.writeFile(dbFilePath, b, written -> {
                                if(written.succeeded()) {
                                    fut.complete();
                                } else {
                                    fut.fail(written.cause());
                                }
                            });
                        } else {
                            fut.fail(created.cause());
                        }
                    });
                } else {
                    fut.fail(deleted.cause());
                }
            });
        } else {
            fut.fail(http.cause());
        }
    }
}


