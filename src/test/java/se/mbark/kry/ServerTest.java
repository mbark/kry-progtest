package se.mbark.kry;

import com.jayway.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;


@RunWith(VertxUnitRunner.class)
public class ServerTest {

    private Vertx vertx;
    private Integer port;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();

        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", port));

        vertx.deployVerticle(Server.class.getName(), options, context.asyncAssertSuccess());
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
        RestAssured.reset();
    }

    @Test
    public void checkThatTheIndexPageIsServed(TestContext context) {
        Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
            context.assertEquals(response.statusCode(), 200);
            context.assertEquals(response.headers().get("content-type"), "text/html");
            async.complete();
        });
    }

    @Test
    public void checkThatServiceIsEmptyFirst(TestContext context) {
        get("/service")
                .then()
                .assertThat()
                .statusCode(200)
                .body("services", empty());
    }

    @Test
    public void checkThatServiceReturnsTheAddedService(TestContext context) {
        given()
                .body("{\"name\":\"test service\", \"url\":\"http://kry.se\"}")
                .when()
                .post("/service")
                .then()
                .assertThat()
                .body("name", equalTo("test service"))
                .body("url", equalTo("http://kry.se"))
                .body("id", notNullValue());
    }

    @Test
    public void checkThatServicesCanBeAdded(TestContext context) {
        int id = given()
                .body("{\"name\":\"test service\", \"url\":\"http://kry.se\"}")
                .when()
                .post("/service")
                .then()
                .assertThat()
                .statusCode(201)
                .extract()
                .jsonPath().getInt("id");


        get("/service")
                .then()
                .assertThat()
                .statusCode(200)
                .body("services[0].id", equalTo(id));
    }

    @Test
    public void checkThatServicesCanBeDeleted(TestContext context) {
        int id = given()
                .body("{\"name\":\"test service\", \"url\":\"http://kry.se\"}")
                .when()
                .post("/service")
                .then()
                .extract()
                .jsonPath().getInt("id");

        delete("/service/" + id);

        get("/service")
                .then()
                .assertThat()
                .body("services", empty());
    }

    @Test
    public void checkThatIdsAreUnique(TestContext context) {
        String id1 = given()
                .body("{\"name\":\"test service\", \"url\":\"http://kry.se\"}")
                .when()
                .post("/service")
                .then()
                .extract()
                .jsonPath().getString("id");

        String id2 = given()
                .body("{\"name\":\"google\", \"url\":\"http://google.se\"}")
                .when()
                .post("/service")
                .then()
                .extract()
                .jsonPath().getString("id");

        context.assertNotEquals(id1, id2);
    }

    @Test
    public void checkThatDeletingNonExistantServicesFail(TestContext context) {
        delete("/service/" + 1)
                .then()
                .assertThat()
                .statusCode(404);
    }

    @Test
    public void checkThatNewlyCreatedTestsReturnNullForCheck(TestContext context) {
        given()
                .body("{\"name\":\"google\", \"url\":\"http://google.se\"}")
                .when()
                .post("/service")
                .then()
                .assertThat()
                .body("status", nullValue())
                .body("lastCheck", nullValue());
    }
}
