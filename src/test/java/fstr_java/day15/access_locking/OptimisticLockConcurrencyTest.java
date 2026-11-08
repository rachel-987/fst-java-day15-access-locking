package fstr_java.day15.access_locking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class OptimisticLockConcurrencyTest {

    private static final String BASE_URL = "http://localhost:8080/inventories";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRejectConcurrentUpdates() throws Exception {

        try (HttpClient client = HttpClient.newHttpClient()) {

            // STEP 1: Tạo Inventory mới
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                              "productName": "iPhone 18",
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

            // STEP 2: Lấy productId và version từ response
            JsonNode inventory = objectMapper.readTree(createResponse.body());

            long productId = inventory.get("productId").asLong();
            long version = inventory.get("version").asLong();

            System.out.println("Created productId: " + productId);
            System.out.println("Initial version: " + version);

            String updateUrl = BASE_URL + "/" + productId;

            // STEP 3: Chuẩn bị 2 request đồng thời
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try (var executor = Executors.newFixedThreadPool(2)) {

                var futures = List.of(
                        executor.submit(() -> update(
                                client, updateUrl,
                                "iPhone 18 Pro", version,
                                ready, start
                        )),
                        executor.submit(() -> update(
                                client, updateUrl,
                                "iPhone 18 Plus", version,
                                ready, start
                        ))
                );

                // STEP 4: Đợi 2 thread sẵn sàng
                assertTrue(ready.await(10, TimeUnit.SECONDS));

                // Cho cả hai bắt đầu gửi request
                start.countDown();

                int status1 = futures.get(0).get(30, TimeUnit.SECONDS);

                int status2 = futures.get(1).get(30, TimeUnit.SECONDS);

                System.out.println("Status 1: " + status1);
                System.out.println("Status 2: " + status2);

                // STEP 5: Kiểm tra kết quả
                assertTrue(
                        (status1 == 200 && status2 == 409)
                                || (status1 == 409 && status2 == 200),
                        "Unexpected statuses: "
                                + status1 + ", " + status2
                );
            }
        }
    }

    private int update(
            HttpClient client,
            String url,
            String productName,
            long version,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {

        ready.countDown();
        start.await();

        String body = """
                {
                  "productName": "%s",
                  "version": %d
                }
                """.formatted(productName, version);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println(
                productName + " -> " + response.statusCode()
                        + " | " + response.body()
        );

        return response.statusCode();
    }
}