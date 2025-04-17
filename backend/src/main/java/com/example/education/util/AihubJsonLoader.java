package com.example.education.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class AihubJsonLoader {

    public String extractPromptText(String grade, String filename) throws IOException {
        File file = new ClassPathResource("aihub/element/" + grade + "/" + filename).getFile();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file);

// OCR_info 배열 확인 후, 첫 번째 요소에서 question_text 추출
        if (root.has("OCR_info")) {
            JsonNode ocrNode = root.get("OCR_info").get(0);
            JsonNode textNode = ocrNode.get("question_text");
            System.out.println(textNode);
            if (textNode != null && !textNode.isNull()) {
                return textNode.asText();
            }
        }
        return "응용 문제를 생성할 수 없습니다.";

    }
}