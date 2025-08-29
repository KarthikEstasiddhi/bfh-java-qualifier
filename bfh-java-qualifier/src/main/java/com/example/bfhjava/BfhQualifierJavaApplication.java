package com.example.bfhjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class BfhQualifierJavaApplication implements ApplicationRunner {

    @Value("${app.name}")   private String name;
    @Value("${app.regNo}")  private String regNo;
    @Value("${app.email}")  private String email;
    @Value("${app.finalQuery:}") private String finalQueryProp;

    private final WebClient webClient;

    public BfhQualifierJavaApplication(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://bfhldevapigw.healthrx.co.in").build();
    }

    public static void main(String[] args) {
        SpringApplication.run(BfhQualifierJavaApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("=== BFH Qualifier Flow Starting ===");
        System.out.printf("Using regNo=%s, name=%s, email=%s%n", regNo, name, email);

        try {
            // 1) Generate webhook + access token
            GenerateWebhookRequest req = new GenerateWebhookRequest(name, regNo, email);
            GenerateWebhookResponse resp = webClient.post()
                    .uri("/hiring/generateWebhook/JAVA")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(GenerateWebhookResponse.class)
                    .block(Duration.ofSeconds(30));

            if (resp == null || resp.accessToken == null) {
                System.err.println("Failed to get accessToken/webhook from API. Aborting.");
                return;
            }

            String derived = deriveQuestion(regNo);
            System.out.println(derived);

            // 2) Resolve final SQL query (from application.yml or final-query.sql)
            String finalQuery = resolveFinalQuery();
            System.out.println("Final SQL to submit: " + finalQuery);

            // 3) Store the result locally (simple 'store' step)
            Path out = Paths.get("target", "final-query.sql");
            Files.createDirectories(out.getParent());
            Files.writeString(out, finalQuery, StandardCharsets.UTF_8);
            System.out.println("Saved final-query.sql at: " + out.toAbsolutePath());

            // 4) Submit to webhook (prefer resp.webhook, fallback to default)
            String webhook = (resp.webhook != null && !resp.webhook.isBlank())
                    ? resp.webhook
                    : "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

            String submissionResp = WebClient.create()
                    .post()
                    .uri(webhook)
                    .header(HttpHeaders.AUTHORIZATION, resp.accessToken) // As instructed: token directly in header
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("finalQuery", finalQuery))
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(ex -> {
                        System.err.println("Submission failed: " + ex.getMessage());
                        return Mono.just("Submission failed: " + ex.getMessage());
                    })
                    .block(Duration.ofSeconds(30));

            System.out.println("Submission response: " + submissionResp);
            System.out.println("=== BFH Qualifier Flow Completed ===");

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String deriveQuestion(String regNo) {
        String digits = regNo.replaceAll("\\D", "");
        String last2 = digits.length() >= 2 ? digits.substring(digits.length() - 2) : digits;
        if (last2.isEmpty()) return "Could not parse last two digits from regNo. Manually check your question link.";
        try {
            int val = Integer.parseInt(last2);
            boolean odd = (val % 2) == 1;
            return "RegNo last two digits = " + last2 + " -> " + (odd ? "Question 1 (Odd)" : "Question 2 (Even)");
        } catch (NumberFormatException e) {
            return "Could not parse last two digits from regNo. Manually check your question link.";
        }
    }

    private String resolveFinalQuery() throws Exception {
        if (finalQueryProp != null && !finalQueryProp.isBlank()) {
            return finalQueryProp.trim();
        }
        Path p = Paths.get("final-query.sql");
        if (Files.exists(p)) {
            return Files.readString(p).trim();
        }
        // Placeholder so the app still runs; replace this with your real SQL
        return "SELECT 1";
    }

    // DTOs
    static record GenerateWebhookRequest(String name, String regNo, String email) {}

    static class GenerateWebhookResponse {
        public String webhook;
        public String accessToken;
    }
}
