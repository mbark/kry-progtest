package se.mbark.kry;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.lang.rxjava.InternalHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class BackgroundServiceTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void checkThatAnEmptyArrayCanBePinged(TestContext context) {
        Async async = context.async();
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        obj.put(DbFile.SERVICES_KEY, arr);

        BackgroundService service = new BackgroundService();
        service.pingAllServices(supplier -> {
            supplier.handle(InternalHelper.result(obj));
        }, consumer -> {
            context.assertTrue(consumer.succeeded());
            context.assertEquals(obj, consumer.result());
            async.complete();
        });
    }

    @Test
    public void checkThatLastCheckedIsUpdatedIfOk(TestContext context) {
        Async async = context.async();

        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();
        JsonObject s = new Service("google","http://localhost:8080").toJson();
        arr.add(s);
        obj.put(DbFile.SERVICES_KEY, arr);

        HttpServer server = vertx.createHttpServer();
        BackgroundService service = new BackgroundService();
        server.requestHandler(request -> {
            request.response().setStatusCode(200).end();
        });

        server.listen(8080, res -> {
            service.pingAllServices(supplier -> {
                supplier.handle(InternalHelper.result(obj));
            }, consumer -> {
                context.assertTrue(consumer.succeeded());
                JsonArray services = consumer.result().getJsonArray(DbFile.SERVICES_KEY);
                context.assertEquals(1, services.size());

                JsonObject updatedService = services.getJsonObject(0);
                context.assertNotNull(updatedService.getString("lastCheck"));
                context.assertNotNull(updatedService.getString("status"));

                async.complete();
            });
        });

    }
}
