package se.mbark.kry;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.lang.rxjava.InternalHelper;

public class DbFile {
    public static final String DB_FILE_PATH = "db.json";
    public static final String SERVICES_KEY = "services";

    private final FileSystem fileSystem;

    public DbFile(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public void getServices(Handler<AsyncResult<JsonObject>> callback) {
        try {
            fileSystem.readFile(DB_FILE_PATH, read -> {
                if(read.succeeded()) {
                    JsonObject contents = new JsonObject(read.result().toString());
                    callback.handle(InternalHelper.result(contents));
                } else {
                    callback.handle(InternalHelper.failure(read.cause()));
                }

            });
        } catch (Throwable t) {
            callback.handle(InternalHelper.failure(t));
        }
    }

    public void updateServices(JsonObject json, Handler<AsyncResult<Void>> callback) {
        Buffer b = Buffer.buffer();
        b.appendString(json.encodePrettily());
        fileSystem.writeFile(DB_FILE_PATH, b, write -> {
            callback.handle(write);
        });

    }

    public void addService(Service service, Handler<AsyncResult<Void>> callback) {
        getServices(read -> {
            if(read.succeeded()) {
                JsonObject json = read.result();

                JsonArray services = json.getJsonArray(SERVICES_KEY);
                services.add(service.toJson());
                json.put(SERVICES_KEY, services);

                updateServices(json, callback);
            } else {
                callback.handle(InternalHelper.failure(read.cause()));
            }
        });
    }

    private boolean deleteServiceById(JsonArray services, int id) {
        for (int i = services.size() - 1; i >= 0; i--) {
            JsonObject o = services.getJsonObject(i);
            if(o.getInteger("id") == id) {
                services.remove(i);
                return true;
            }
        }
        return false;
    }

    public void deleteService(int serviceId, Handler<AsyncResult<Void>> callback) {
        getServices(read -> {
            if(read.succeeded()) {
                JsonObject json = read.result();
                JsonArray services = json.getJsonArray(SERVICES_KEY);

                boolean deleted = deleteServiceById(services, serviceId);
                if(!deleted) {
                    callback.handle(InternalHelper.failure(new Exception("Id not found")));
                    return;
                }

                json.put(SERVICES_KEY, services);
                updateServices(json, callback);
            } else {
                callback.handle(InternalHelper.failure(read.cause()));
            }
        });
    }

    public void createYourself(Future<Void> fut) {
        fileSystem.delete(DB_FILE_PATH, deleted -> {
            if(deleted.succeeded()) {
                fileSystem.createFile(DB_FILE_PATH, created -> {
                    if(created.succeeded()) {
                        Buffer b  = Buffer.buffer();
                        b.appendString("{\"services\": [] }");
                        fileSystem.writeFile(DB_FILE_PATH, b, written -> {
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
    }
}
