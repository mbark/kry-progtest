package se.mbark.kry;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mbark on 31/03/16.
 */
public class Service {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int id;
    private String name;
    private String url;
    private String status;
    private String lastCheck;

    public Service(String name, String url) {
        this.id = COUNTER.getAndIncrement();
        this.name = name;
        this.url = url;
    }

    public Service(int id) {
        this.id = id;
    }

    public Service() {
        this.id = COUNTER.getAndIncrement();
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getLastCheck() {
        return lastCheck;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastCheck(String lastCheck) {
        this.lastCheck = lastCheck;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("id", id)
                .put("name", name)
                .put("url", url)
                .put("status", status)
                .put("lastCheck", lastCheck);
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        return id == service.id;

    }

    @Override
    public int hashCode() {
        return id;
    }


}
