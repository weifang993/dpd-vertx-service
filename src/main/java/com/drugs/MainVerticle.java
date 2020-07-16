package com.drugs;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.mongo.HashAlgorithm;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.auth.mongo.MongoAuthOptionsConverter;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.bson.conversions.Bson;
import io.vertx.ext.auth.mongo.MongoAuthOptions;

import java.util.HashSet;
import java.util.Set;

import static com.mongodb.client.model.Filters.regex;

public class MainVerticle extends AbstractVerticle {
  private MongoClient mongoClient;
  private MongoAuth authProvider;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    // allowedHeaders.add("X-PINGARUNER");

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    // system variables
    String HOST = System.getenv("MONGODB_SERVICE_HOST");
    if(HOST == null) HOST = "localhost";
    String PORT = System.getenv("MONGODB_SERVICE_PORT");
    if(PORT == null) PORT = "27017";
    String user = System.getenv("MONGODB_USER");
    String password = System.getenv("MONGODB_PASSWORD");

    // mongodb config
    JsonObject config = new JsonObject()
      .put("connection_string", "mongodb://" + HOST + ":" + PORT)
      .put("db_name", "dpd");

    mongoClient = MongoClient.createShared(vertx, config);
    authProvider = MongoAuth.create(mongoClient, new JsonObject());
    authProvider.getHashStrategy().setAlgorithm(HashAlgorithm.PBKDF2);

    // routes for native data format
    router.get("/api/drugs/brand_name/:brand").handler(this::handleGetDrugByBrandName);

    vertx.createHttpServer().requestHandler(router::handle).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void handleGetDrugByBrandName(RoutingContext routingContext) {
    String brand = routingContext.request().getParam("brand");

    HttpServerResponse response = routingContext.response();
    if (brand == null) {
      sendError(400, response);
    } else {
      JsonObject userInfo = new JsonObject()
              .put("username", "dpd")
              .put("password", "dpd");
      authProvider.authenticate(userInfo, authenRes -> {
        if (authenRes.succeeded()) {
          JsonObject query = new JsonObject().put("brandName", new JsonObject().put("$regex", ".*" + brand.toUpperCase() + ".*"));
          mongoClient.find("active_drugs", query, res -> {
                    if (res.succeeded()) {
                      System.out.println("query succeeded. found: " + res.result().size());
                      JsonArray drugs = new JsonArray();
                      for (JsonObject json : res.result()) {
                        drugs.add(json);
                      }
                      response.end(drugs.encodePrettily());
                    } else {
                      res.cause().printStackTrace();
                    }
                  }
          );
        } else {
          System.out.println("Faield to authenticate to mongodb");
        }
      });
    }
  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }
}
