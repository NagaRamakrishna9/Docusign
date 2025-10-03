package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import java.util.Map;
@RestController
@RequestMapping("/docusign")
public class DocusignController {
    @Autowired
    private DocuSignService docuSignService;
    /**
     * Full flow: Create envelope + get sender view URL
     */
    @PostMapping("/envelopes/sender-view")
    public Map<String, Object> createEnvelopeAndGetSenderView(@RequestBody Map<String, Object> envelopeRequest) {
        WebClient webClient = docuSignService.getWebClient();
        String accountId = docuSignService.getAccountId();
        // 1️⃣ Create envelope
        Map<String, Object> envelopeResponse = webClient.post()
                .uri("/v2.1/accounts/{accountId}/envelopes", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(envelopeRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        String envelopeId = (String) envelopeResponse.get("envelopeId");
        // 2️⃣ Prepare sender view request
        Map<String, Object> senderViewRequest = Map.of(
                "returnUrl", "https://yourapp.com/docusign/return" // Replace with your app URL
        );
        // 3️⃣ Get sender view URL
        Map<String, Object> senderViewResponse = webClient.post()
                .uri("/v2.1/accounts/{accountId}/envelopes/{envelopeId}/views/sender", accountId, envelopeId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(senderViewRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        // 4️⃣ Return envelope info + sender URL
        return Map.of(
                "envelopeId", envelopeId,
                "status", envelopeResponse.get("status"),
                "senderUrl", senderViewResponse.get("url")
        );
    }
    /**
     * Optional: If you want to keep separate endpoints for envelope creation
     */
    @PostMapping("/envelopes")
    public Map<String, Object> createEnvelope(@RequestBody Map<String, Object> envelopeRequest) {
        WebClient webClient = docuSignService.getWebClient();
        String accountId = docuSignService.getAccountId();
        return webClient.post()
                .uri("/v2.1/accounts/{accountId}/envelopes", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(envelopeRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
    /**
     * Optional: If you want to keep separate endpoint for sender view
     */
    @PostMapping("/envelopes/{envelopeId}/views/sender")
    public Map<String, Object> getSenderView(
            @PathVariable String envelopeId,
            @RequestBody Map<String, Object> senderViewRequest) {
        WebClient webClient = docuSignService.getWebClient();
        String accountId = docuSignService.getAccountId();
        return webClient.post()
                .uri("/v2.1/accounts/{accountId}/envelopes/{envelopeId}/views/sender", accountId, envelopeId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(senderViewRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
    @GetMapping("/envelopes")
    public Map<String, Object> listEnvelopes(
            @RequestParam(defaultValue = "sent") String fromDate 
    ) {
        WebClient webClient = docuSignService.getWebClient();
        String accountId = docuSignService.getAccountId();
        // DocuSign requires a from_date filter for listing envelopes
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2.1/accounts/{accountId}/envelopes")
                        .queryParam("from_date", fromDate) 
                        .build(accountId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

   @PostMapping("/envelopes/signature-text")
public Map<String, Object> createEnvelopeWithSignatureText(@RequestBody Map<String, Object> request) {
    WebClient webClient = docuSignService.getWebClient();
    String accountId = docuSignService.getAccountId();

    String base64Document = (String) request.get("documentBase64"); // PDF as Base64
    String documentName = (String) request.get("documentName");
    String recipientName = (String) request.get("recipientName");
    String recipientEmail = (String) request.get("recipientEmail");

    Map<String, Object> envelopeRequest = Map.of(
        "emailSubject", "Please sign this document",
        "documents", List.of(Map.of(
            "documentBase64", base64Document,
            "name", documentName,
            "fileExtension", "pdf",
            "documentId", "1"
        )),
        "recipients", Map.of(
            "signers", List.of(Map.of(
                "email", recipientEmail,
                "name", recipientName,
                "recipientId", "1",
                "tabs", Map.of(
                    "signHereTabs", List.of(Map.of(
                        "anchorString", "Signature",
                        "anchorUnits", "pixels",
                        "anchorXOffset", "0",
                        "anchorYOffset", "0"
                    ))
                )
            ))
        ),
        "status", "sent" // Automatically sends email
    );

    // Call DocuSign API to create envelope
    Map<String, Object> envelopeResponse = webClient.post()
            .uri("/v2.1/accounts/{accountId}/envelopes", accountId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(envelopeRequest)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

    return envelopeResponse;
}

}
