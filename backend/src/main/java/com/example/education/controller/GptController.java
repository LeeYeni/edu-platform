package com.example.education.controller;

import com.example.education.dto.GptResponseDto;
import com.example.education.dto.QuizLogRequest;
import com.example.education.service.GptService;
import com.example.education.service.QuestionService;
import com.example.education.util.AihubJsonLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class GptController {

    private final GptService gptService;
    private final QuestionService questionService;
    private final AihubJsonLoader jsonLoader;

    private static final String PROMPT_PREFIX =
            "아래의 대단원, 중단원, 소단원에 해당하는 초등학교 수학 문제를 생성해줘.\n" +
                    "조건:\n" +
                    "1. 문제 개수는 정확히 N개 생성해줘. (아래 [문제 개수]를 참고해)\n" +
                    "2. 문제는 객관식(4지선다), OX, 주관식 중 하나로 생성해줘.\n" +
                    "- 객관식인 경우, options의 id별 text가 모두 달라야만 해.\n" +
                    "- OX인 경우, answer이 false일 땐 explanation에 정답 풀이를 작성하고, text에는 오답을 적어줘.\n" +
                    "3. 문제 유형은 다음 중 하나로 지정해줘: 'multiple', 'truefalse', 'subjective'\n" +
                    "4. **answer는 반드시 정답으로 설정한 보기 또는 값과 정확히 일치해야 해.**\n" +
                    "- 예: 계산 결과가 false인데 answer가 true면 안 돼.\n" +
                    "- 숫자 계산 또는 판단 결과를 기준으로 정확한 논리값(true/false), 보기 ID(a/b/...), 또는 정확한 주관식 텍스트를 넣어야 해.\n" +
                    "\n" +
                    "문제는 아래 JSON 형식 중 하나로 생성해줘. 그리고 최종 결과는 반드시 배열([])로 출력해줘.\n" +
                    "\n" +
                    "객관식 예시:\n" +
                    "{\n" +
                    "  \"id\": \"q1\",\n" +
                    "  \"type\": \"multiple\",\n" +
                    "  \"text\": \"질문 내용\",\n" +
                    "  \"options\": [\n" +
                    "    { \"id\": \"a\", \"text\": \"보기 1\" },\n" +
                    "    { \"id\": \"b\", \"text\": \"보기 2\" },\n" +
                    "    { \"id\": \"c\", \"text\": \"보기 3\" },\n" +
                    "    { \"id\": \"d\", \"text\": \"보기 4\" }\n" +
                    "  ],\n" +
                    "  \"answer\": \"b\",\n" +
                    "  \"explanation\": \"해설\"\n" +
                    "}\n" +
                    "\n" +
                    "OX 예시:\n" +
                    "{\n" +
                    "  \"id\": \"q2\",\n" +
                    "  \"type\": \"truefalse\",\n" +
                    "  \"answer\": true,\n" +
                    "  \"text\": \"질문 내용\",\n" +
                    "  \"explanation\": \"해설\"\n" +
                    "}\n" +
                    "\n" +
                    "주관식 예시:\n" +
                    "{\n" +
                    "  \"id\": \"q3\",\n" +
                    "  \"type\": \"subjective\",\n" +
                    "  \"text\": \"질문 내용\",\n" +
                    "  \"answer\": \"정답\",\n" +
                    "  \"explanation\": \"해설\"\n" +
                    "}\n" +
                    "\n" +
                    "위 형식에 따라 문제들을 JSON 배열로 묶어서 출력해줘.\n" +
                    "**중요**: 속성명은 반드시 큰따옴표로 감싸고, 최종 결과는 유효한 JSON 배열이어야 해.";




    @PostMapping("/log")
    public GptResponseDto generateFromQuizLog(@RequestBody QuizLogRequest req) {
        try {
            // 1. 학년/단원 → 경로, 파일 prefix 추출
            // String gradeFolder = convertGradeToFolder(req.getGrade());

            // String prompt = jsonLoader.extractPromptText(gradeFolder, "P3_1_01_00040_00474.json");
            String prompt = "[대단원] " + req.getChapter() + ", [중단원] " + req.getMiddle() + ", [소단원] " + req.getSmall() + "[문제 개수] " + req.getNumberOfProblems() + "개";
            String response = gptService.getGptResponse(PROMPT_PREFIX + prompt);

            String questionId = questionService.saveQuestionsFromGptResponse(req.getUserId(), req.getUserType(), req.getChapter(), req.getMiddle(), req.getSmall(), response);

            return new GptResponseDto(prompt, response, questionId);

        } catch (Exception e) {
            return new GptResponseDto("오류 발생", "❌ 응용 문제 생성 실패: " + e.getMessage(), null);
        }
    }
}
