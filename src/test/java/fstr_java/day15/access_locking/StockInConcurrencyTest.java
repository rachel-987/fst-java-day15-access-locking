package fstr_java.day15.access_locking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StockInConcurrencyTest {

    private static final String BASE_URL = "http://localhost:8080/inventories";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldHandleConcurrentStockIn() throws Exception {

        int requestCount = 10;
        int stockInQuantity = 10;
        int initialQuantity = 100;

        try (HttpClient client = HttpClient.newHttpClient()) {

            // STEP 1: Tạo Inventory mới
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                              "productName": "Stock In Concurrency Test",
                              "quantity": 100
                            }
                            """))
                    .build();

            HttpResponse<String> createResponse = client.send(
                    createRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(201, createResponse.statusCode(),
                    createResponse.body());

            JsonNode created = objectMapper.readTree(createResponse.body());

            long productId = created.get("productId").asLong();

            System.out.println("Created productId: " + productId);

            // STEP 2: Chuẩn bị 10 request stock-in
            String stockInUrl = BASE_URL + "/" + productId + "/stock-in";

            // CountDownLatch: Cho 10 request bắt đầu gần như đồng thời
            CountDownLatch ready = new CountDownLatch(requestCount);
            CountDownLatch start = new CountDownLatch(1);

            List<Integer> statuses = new ArrayList<>();

            try (var executor = Executors.newFixedThreadPool(requestCount)) {

                var futures = new ArrayList<java.util.concurrent.Future<Integer>>();

                // Mỗi thread gọi ready.countDown() để báo đã sẵn sàng, rồi chờ tại start.await().
                for (int i = 0; i < requestCount; i++) {

                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(stockInUrl))
                                .header("Content-Type", "application/json")
                                .timeout(Duration.ofSeconds(30))
                                .POST(HttpRequest.BodyPublishers.ofString(
                                        "{\"quantity\":10}"
                                ))
                                .build();

                        HttpResponse<String> response = client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                        System.out.println(
                                "Stock-in status: "
                                        + response.statusCode()
                        );

                        return response.statusCode();
                    }));
                }

                assertTrue(ready.await(10, TimeUnit.SECONDS));

                // Main thread chờ cả 10 task sẵn sàng, sau đó gọi start.countDown() để thả tất cả cùng gửi HTTP request.
                start.countDown();

                for (var future : futures) {
                    statuses.add(future.get(35, TimeUnit.SECONDS));
                }
            }

            // STEP 3: Kiểm tra cả 10 request thành công
            long successCount = statuses.stream()
                    .filter(status -> status == 200)
                    .count();

            assertEquals(requestCount, successCount,
                    "Unexpected HTTP statuses: " + statuses);

            // STEP 4: Đọc lịch sử từ Envers
            HttpRequest historyRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            BASE_URL + "/" + productId + "/history"
                    ))
                    .GET()
                    .build();

            HttpResponse<String> historyResponse = client.send(
                    historyRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(200, historyResponse.statusCode(),
                    historyResponse.body());

            JsonNode history =
                    objectMapper.readTree(historyResponse.body());

            assertTrue(history.isArray());
            assertEquals(requestCount + 1, history.size(),
                    "Expected 1 INSERT + 10 UPDATE revisions");

            // STEP 5: Kiểm tra quantity cuối cùng
            JsonNode latestRevision =
                    history.get(history.size() - 1);

            int actualQuantity =
                    latestRevision.get("quantity").asInt();

            int expectedQuantity =
                    initialQuantity + requestCount * stockInQuantity;

            System.out.println("Expected quantity: " + expectedQuantity);
            System.out.println("Actual quantity: " + actualQuantity);

            assertEquals(expectedQuantity, actualQuantity,
                    "Lost Update detected!");
        }
    }
}