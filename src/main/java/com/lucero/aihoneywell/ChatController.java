package com.lucero.aihoneywell;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@CrossOrigin(origins = "http://localhost:8081")
@RestController("/")
public class ChatController{

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final Resource promptTemplate;
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);



    public ChatController(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, @Value("classpath:/prompts/prompt-references2.st") Resource promptTemplate ) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
    }

    @PostMapping("/chat")
    public ResponseEntity<Object> chatClientAsking(@RequestParam(value = "message") String message){

        String userText = message;

        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(50).build());
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(promptTemplate);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("input", userText, "documents", results));
        Prompt prompt = new Prompt(systemMessage.getText());

        String response = Objects.requireNonNull(chatClient.prompt(prompt).call().content());
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }
}