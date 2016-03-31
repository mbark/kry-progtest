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
            .parameters("name", "test service", "url", "http://kry.se")
            .when()
            .post("/service")
            .then()
            .assertThat()
            .body("name", equalTo("test service"))
            .body("url", equalTo("http://kry.se"))
            .body("id", hasKey("id"))
            .extract()
            .jsonPath().getString("id");
  }

  @Test
  public void checkThatServicesCanBeAdded(TestContext context) {
    given()
            .parameters("name", "test service", "url", "http://kry.se")
            .when()
            .post("/service")
            .then()
            .assertThat()
            .statusCode(201)
            .extract()
            .jsonPath().getString("id");


    get("/service")
            .then()
            .assertThat()
            .statusCode(200)
            .body("services", arrayWithSize(1))
            .body("services[0].id", equalTo("id"));
  }

  @Test
  public void checkThatServiceReturnsTheDeletedService(TestContext context) {
    String id = given()
            .parameters("name", "test service", "url", "http://kry.se")
            .when()
            .post("/service")
            .then()
            .extract()
            .jsonPath().getString("service.id");

    delete("/service/" + id)
            .then()
            .assertThat()
            .body("name", equalTo("test service"))
            .body("url", equalTo("http://kry.se"))
            .body("id", equalTo(id));
  }

  @Test
  public void checkThatServicesCanBeDeleted(TestContext context) {
    String id = given()
            .parameters("name", "test service", "url", "http://kry.se")
            .when()
            .post("/service")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .jsonPath().getString("service.id");

    delete("/service/" + id);

    get("/service")
            .then()
            .assertThat()
            .body("services", empty());
  }

    @Test
    public void checkThatIdsAreUnique(TestContext context) {
        String id1 = given()
                .parameters("name", "test service", "url", "http://kry.se")
                .when()
                .post("/service")
                .then()
                .extract()
                .jsonPath().getString("service.id");

        String id2 = given()
                .parameters("name", "google", "url", "http://google.se")
                .when()
                .post("/service")
                .then()
                .extract()
                .jsonPath().getString("service.id");

        context.assertNotEquals(id1, id2);
    }

    @Test
    public void checkThatDeletingNonExistantServicesFail(TestContext context) {
        delete("/service/" + "this_id_does_not_exist")
        .then()
        .assertThat()
        .statusCode(404);
    }

    @Test
    public void checkThatNewlyCreatedTestsReturnNullForCheck(TestContext context) {
        given()
                .parameters("name", "test service", "url", "http://kry.se")
                .when()
                .post("/service")
                .then()
                .assertThat()
                .body("status", nullValue())
                .body("lastCheck", nullValue());
    }
}
