package com.lucero.aihoneywell;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@RestController("/")
public class ChatController{

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final Resource promptTemplate;

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);



    public ChatController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/prompt-references.st") Resource promptTemplate) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
    }


    @PostMapping("/chat")
    public String chatClientAsking(@RequestParam(value = "message") String message){

        String userText = message;
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder().query("message").topK(15).build());
        log.info(message);
        log.info("Resultados da query {}", results);
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(promptTemplate);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("input", userText, "documents", results));
        log.info(systemMessage.toString());
        Prompt prompt = new Prompt(systemMessage);

        return Objects.requireNonNull(chatClient.prompt(prompt).call().chatResponse()).getResults().toString();
    }
}
