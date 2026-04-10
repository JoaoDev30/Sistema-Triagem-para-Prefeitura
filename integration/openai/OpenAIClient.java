package br.gov.alvara.integration.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAIClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    @Value("${openai.api.max-tokens}")
    private int maxTokens;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envia um prompt para a API da OpenAI e retorna a resposta em texto.
     * O prompt deve instruir a IA a retornar JSON válido.
     */
    public String enviarPrompt(String promptSistema, String promptUsuario) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0.2,   // Baixa temperatura = respostas mais determinísticas
                "messages", List.of(
                        Map.of("role", "system", "content", promptSistema),
                        Map.of("role", "user", "content", promptUsuario)
                )
        );

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            log.debug("Enviando requisição para OpenAI: model={}", model);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<?> choices = (List<?>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    String conteudo = (String) message.get("content");
                    log.debug("Resposta recebida da OpenAI com {} caracteres", conteudo.length());
                    return conteudo;
                }
            }
            throw new RuntimeException("Resposta inesperada da OpenAI: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Erro na comunicação com a OpenAI: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na integração com a IA: " + e.getMessage(), e);
        }
    }
}
