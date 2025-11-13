package com.ekaterina.avito;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AvitoApiTests {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "https://qa-internship.avito.com";
    }
    //Валидное объявление для позитивных тестов, возвращает ID
    private String createItemAndGetId(int sellerId) {
        Map<String, Object> body = new HashMap<>();
        body.put("sellerId", sellerId);
        body.put("name", "Test ad " + sellerId);
        body.put("price", 100);

        Map<String, Integer> stats = new HashMap<>();
        stats.put("likes", 1);
        stats.put("viewCount", 1);
        stats.put("contacts", 1);
        body.put("statistics", stats);

        String status = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/1/item")
                .then()
                .statusCode(200)
                .body("status", notNullValue())
                .extract()
                .path("status");

        //Сохранение объявления
        return status.split(" - ")[1];
    }

    //1. Создание объявления

    //Создание валидного объявления
    @Test
    void createItem_shouldReturn200AndId() {
        int sellerId = 794700;

        Map<String, Object> body = new HashMap<>();
        body.put("sellerId", sellerId);
        body.put("name", "Test create");
        body.put("price", 1500);

        Map<String, Integer> stats = new HashMap<>();
        stats.put("likes", 1);
        stats.put("viewCount", 10);
        stats.put("contacts", 1);
        body.put("statistics", stats);

        String status = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/1/item")
                .then()
                .statusCode(200)
                .body("status", containsString("Сохранили объявление"))
                .extract()
                .path("status");

        String id = status.split(" - ")[1];
        assertNotNull(id);
        assertEquals(36, id.length());
    }

    //Создание объявления без поля name
    @Test
    void createItem_withoutName_shouldReturn400() {
        int sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);

        Map<String, Object> body = new HashMap<>();
        body.put("sellerId", sellerId);
        body.put("price", 100);

        Map<String, Integer> stats = new HashMap<>();
        stats.put("likes", 1);
        stats.put("viewCount", 1);
        stats.put("contacts", 1);
        body.put("statistics", stats);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/1/item")
                .then()
                .statusCode(400);
    }

    //Создание объявления без поля sellerId
    @Test
    void createItem_withoutSellerId_shouldReturn400() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "No seller");
        body.put("price", 100);

        Map<String, Integer> stats = new HashMap<>();
        stats.put("likes", 1);
        stats.put("viewCount", 1);
        stats.put("contacts", 1);
        body.put("statistics", stats);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/1/item")
                .then()
                .statusCode(400);
    }

    //Передать price строкой
    @Test
    void createItem_priceAsString_shouldReturn400() {
        int sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);

        Map<String, Object> body = new HashMap<>();
        body.put("sellerId", sellerId);
        body.put("name", "Price as string");
        body.put("price", "hello"); // некорректный тип

        Map<String, Integer> stats = new HashMap<>();
        stats.put("likes", 1);
        stats.put("viewCount", 1);
        stats.put("contacts", 1);
        body.put("statistics", stats);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/1/item")
                .then()
                .statusCode(400);
    }

    //TC18: Отсутствует поле statistics
    @Test
    void createItem_withoutStatistics_shouldReturn400() {
        int sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);

        Map<String, Object> body = new HashMap<>();
        body.put("sellerId", sellerId);
        body.put("name", "No statistics");
        body.put("price", 100);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/1/item")
                .then()
                .statusCode(400);
    }

    //2. Получить объявление по ID

    //Получение существующего объявления
    @Test
    void getItemById_shouldReturnCreatedItem() {
        int sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);
        String createdId = createItemAndGetId(sellerId);

        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/1/item/" + createdId)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(createdId))
                .body("[0].sellerId", equalTo(sellerId))
                .body("[0].name", notNullValue())
                .body("[0].price", notNullValue())
                .body("[0].statistics.likes", notNullValue())
                .body("[0].statistics.viewCount", notNullValue())
                .body("[0].statistics.contacts", notNullValue())
                .body("[0].createdAt", notNullValue());
    }

    //Получение объявления по несуществующему id
    @Test
    void getItemByNonExistingId_shouldReturn404() {
        String fakeId = UUID.randomUUID().toString();

        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/1/item/" + fakeId)
                .then()
                .statusCode(404);
    }


    //3. Получить все объявления по sellerId

    //TC10: Получить все объявления существующего продавца
    @Test
    void getItemsBySeller_shouldReturnListOfItems() {
        int sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);
        createItemAndGetId(sellerId);
        createItemAndGetId(sellerId);

        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/1/" + sellerId + "/item")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("sellerId", everyItem(equalTo(sellerId)));
    }

    //Получить объявления для продавца без объявлений
    @Test
    void getItemsBySeller_withoutItems_shouldReturnEmptyArray() {
        int sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);

        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/1/" + sellerId + "/item")
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    //4. Получить статистику по объявлению

    //Получение статистики существующего объявления
    @Test
    void getStatisticById_shouldReturnStatsArray() {
        int sellerId = ThreadLocalRandom.current().nextInt(111111, 999999);
        String createdId = createItemAndGetId(sellerId);

        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/1/statistic/" + createdId)
                .then()
                .statusCode(200)
                .body("$", notNullValue())
                .body("$", not(empty()))
                .body("[0].likes", notNullValue())
                .body("[0].viewCount", notNullValue())
                .body("[0].contacts", notNullValue());
    }

    //Получение статистики по несуществующему id
    @Test
    void getStatisticByNonExistingId_shouldReturn404() {
        String fakeId = UUID.randomUUID().toString();

        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/1/statistic/" + fakeId)
                .then()
                .statusCode(404);
    }
}
