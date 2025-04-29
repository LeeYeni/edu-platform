package com.example.education.controller;

import com.example.education.dto.GptResponseDto;
import com.example.education.util.GptResponseValidator;
import com.example.education.dto.QuizLogRequest;
import com.example.education.service.GptService;
import com.example.education.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class GptController {

    private final GptService gptService;
    private final QuestionService questionService;

    private static final String PROMPT_PREFIX =
            "아래의 대단원, 중단원, 소단원에 해당하는 초등학교 수학 문제를 생성해줘.\n" +
                    "※ 참고: 대한민국 초등학교 3-4학년 수학 성취기준 전체\n" +

                    "<성취기준 전문>\n" +
                    "Ⅰ. 수와 연산\n" +
                    "Ⅰ. 다섯 자리 이상의 수\n" +
                    "[4수01-01] 큰 수의 필요성을 인식하면서 10000 이상의 큰 수에 대한 자릿값과 위치적 기수법을 이해하고, 수를 읽고 쓸 수 있다.\n" +
                    "[4수01-02] 다섯 자리 이상의 수의 범위에서 수의 계열을 이해하고, 수의 크기를 비교하며 그 방법을 설명할 수 있다.\n" +
                    "\n" +
                    "Ⅱ. 세 자리 수의 덧셈과 뺄셈\n" +
                    "[4수01-03] 세 자리 수의 덧셈과 뺄셈의 계산 원리를 이해하고 그 계산을 할 수 있다.\n" +
                    "\n" +
                    "Ⅲ. 세 자리 수 범위의 곱셈\n" +
                    "[4수01-04] 곱하는 수가 한 자리 수 또는 두 자리 수인 곱셈의 계산 원리를 이해하고 그 계산을 할 수 있다.\n" +
                    "\n" +
                    "Ⅳ. 세 자리 수 범위의 나눗셈\n" +
                    "[4수01-05] 나눗셈이 이루어지는 실생활 상황과 연결하여 나눗셈의 의미를 알고, 곱셈과 나눗셈의 관계를 이해한다.\n" +
                    "[4수01-06] 나누는 수가 한 자리 수인 나눗셈의 계산 원리를 이해하고 그 계산을 할 수 있으며, 나눗셈에서 몫과 나머지의 의미를 안다.\n" +
                    "[4수01-07] 나누는 수가 두 자리 수인 나눗셈의 계산 원리를 이해하고 그 계산을 할 수 있다.\n" +
                    "\n" +
                    "Ⅴ. 자연수의 어림셈\n" +
                    "[4수01-08] 자연수의 덧셈, 뺄셈, 곱셈, 나눗셈과 관련한 여러 가지 상황에서 어림셈을 할 수 있다.\n" +
                    "\n" +
                    "Ⅵ. 분수\n" +
                    "[4수01-09] 양의 등분할을 통하여 분수의 필요성을 인식하고, 분수를 이해하고 읽고 쓸 수 있다.\n" +
                    "[4수01-10] 단위분수, 진분수, 가분수, 대분수를 알고, 그 관계를 이해한다.\n" +
                    "[4수01-11] 분모가 같은 분수끼리, 단위분수끼리 크기를 비교하고 그 방법을 설명할 수 있다.\n" +
                    "\n" +
                    "Ⅶ. 소수\n" +
                    "[4수01-12] 분모가 10인 진분수와 연결하여 소수 한 자리 수를 이해하고 읽고 쓸 수 있다.\n" +
                    "[4수01-13] 자릿값의 원리를 바탕으로 소수 두 자리 수와 소수 세 자리 수를 이해하고 읽고 쓸 수 있다.\n" +
                    "[4수01-14] 소수의 크기를 비교하고 그 방법을 설명할 수 있다.\n" +
                    "\n" +
                    "Ⅷ. 분수의 덧셈과 뺄셈\n" +
                    "[4수01-15] 분모가 같은 분수의 덧셈과 뺄셈의 계산 원리를 이해하고 그 계산을 할 수 있다.\n" +
                    "\n" +
                    "Ⅸ. 소수의 덧셈과 뺄셈\n" +
                    "[4수01-16] 소수 두 자리 수의 범위에서 소수의 덧셈과 뺄셈의 계산 원리를 이해하고 그 계산을 할 수 있다.\n" +
                    "\n" +
                    "(가) 성취기준 해설\n" +
                    "[4수01-03] 덧셈은 세 자리 수의 범위에서 다루되, 합이 네 자리 수인 경우도 포함한다.\n" +
                    "[4수01-04] 곱셈은 '(두 자리 수) × (한 자리 수)', '(세 자리 수) × (한 자리 수)', '(두 자리 수) × (두 자리 수)', '(세 자리 수) × (두 자리 수)'를 다룬다.\n" +
                    "[4수01-06] 나눗셈에서 '(두 자리 수) ÷ (한 자리 수)'는 나누어떨어지는 경우와 나누어떨어지지 않는 경우를 포함하여 몫과 나머지를 이해하게 한다.\n" +
                    "[4수01-07] 나누는 수가 두 자리 수인 나눗셈에서는 '(두 자리 수) ÷ (두 자리 수)', '(세 자리 수) ÷ (두 자리 수)'를 다룬다.\n" +
                    "[4수01-09] 1보다 작은 양을 나타내는 경우를 통하여 분수의 필요성이나 그 표현의 편리함을 인식하게 할 수 있다. 양의 등분할을 통하여 분수를 도입할 때 부분과 전체를 파악하게 하고, '분모', '분자'를 사용한다.\n" +
                    "\n" +
                    "(나) 성취기준 적용 시 고려 사항\n" +
                    "'수와 연산' 영역에서는 '나눗셈, 몫, 나머지, 나누어떨어진다, 분수, 분모, 분자, 단위분수, 진분수, 가분수, 대분수, 자연수, 소수, 소수점(.), ÷' 등의 용어와 기호를 다룬다.\n" +
                    "뉴스, 광고 등 여러 가지 매체를 활용해 실생활에서 다섯 자리 이상의 큰 수가 쓰이는 경우를 찾아보고, 큰 수에 대한 필요성을 인식하게 한다.\n" +
                    "곱셈식과 나눗셈식으로 상황을 나타내어 곱셈과 나눗셈의 관계를 이해하게 한다.\n" +
                    "나눗셈 검산은 나눗셈식을 곱셈식으로 단순히 변환하는 것보다 검산의 목적과 필요성을 이해하는 데 초점을 맞춘다.\n" +
                    "자연수의 사칙연산 전 어림셈을 하여 계산 결과가 타당한지 확인하고, 어림셈이 필요한 실생활 문제를 해결하는 활동을 한다.\n" +
                    "실생활 속 소수 활용 사례를 통해 소수의 필요성을 인식하고, 소수 덧셈과 뺄셈은 계산 원리 이해 수준에서 간단히 다룬다.\n" +
                    "계산 기능 숙달이 목적이 아닐 경우 계산기를 사용하게 할 수 있다.\n" +
                    "'수와 연산' 영역에서 문제해결 전략을 지도하고, 설명하는 과정에서 다른 친구의 의견을 존중하고 경청하는 태도를 기르게 한다.\n" +

                    "주의사항:\n" +
                    "- 위 성취기준은 '문제 출제 시 참고용'이야.\n" +
                    "너는 초등학교 3-4학년 수학 문제를 만드는 AI야.\n" +
                    "\n" +
                    "[목표]\n" +
                    "- 요청된 대단원/중단원/소단원 주제에 맞는 문제를 생성해.\n" +
                    "- 난이도는 초등학교 3-4학년 수준.\n" +
                    "- 문제 지문, 보기, 해설은 모두 초등학생 눈높이에 맞춰.\n" +
                    "\n" +
                    "[출제 조건]\n" +
                    "1. 문제 수: 정확히 N개 ([문제 개수]에 명시)\n" +
                    "2. 문제 유형: 'multiple'(객관식), 'truefalse'(OX), 'subjective'(주관식) 중 선택\n" +
                    "3. multiple 문제는 보기(options)가 최소 2개 이상 필수\n" +
                    "4. truefalse 문제는 answer가 true 또는 false (boolean 값)\n" +
                    "5. subjective 문제는 answer에 정확한 텍스트 답변\n" +
                    "\n" +
                    "[출력 포맷]\n" +
                    "- 각 문제는 JSON 형식\n" +
                    "- 전체 문제를 JSON 배열([])로 감싸서 출력\n" +
                    "- 속성명은 반드시 큰따옴표(\"\")로 감싸기\n" +
                    "\n" +
                    "[정답(answer)와 해설(explanation) 작성 규칙]\n" +
                    "- 해설은 다음 3단계로 작성: 문제 풀이 → 정답 도출 → 정답 근거 요약\n" +
                    "- 해설은 항상 긍정 표현만 사용 (\"아닙니다\" 금지)\n" +
                    "- '정답'은 반드시 answer로 지정한 보기(option) 또는 값과 일치\n" +
                    "- answer가 'a'라면, 해설에도 'a'가 정답인 이유만 설명해야 함\n" +
                    "- answer와 해설이 불일치하면 문제는 무효 (재출제해야 함)\n" +
                    "\n" +
                    "[검증 규칙]\n" +
                    "- multiple 문제는 보기(options) 중 정확히 하나만 정답이어야 함\n" +
                    "- 복수 정답(multiple answers) 금지\n" +
                    "- options에 정답이 없으면 폐기\n" +
                    "- 문제 개수(N개)와 실제 생성 개수 불일치 시 폐기\n" +
                    "\n" +
                    "[JSON 예시]\n" +
                    "객관식 multiple:\n" +
                    "{\n" +
                    "  \"id\": \"q1\",\n" +
                    "  \"type\": \"multiple\",\n" +
                    "  \"text\": \"문제 지문\",\n" +
                    "  \"options\": [\n" +
                    "    { \"id\": \"a\", \"text\": \"보기1\" },\n" +
                    "    { \"id\": \"b\", \"text\": \"보기2\" },\n" +
                    "    { \"id\": \"c\", \"text\": \"보기3\" },\n" +
                    "    { \"id\": \"d\", \"text\": \"보기4\" }\n" +
                    "  ],\n" +
                    "  \"answer\": \"정답 id\",\n" +
                    "  \"explanation\": \"정답 근거 요약('따라서 정답은 무엇(a, b, c, d 중 하나)입니다.' 반드시 포함)\"\n" +
                    "}\n" +
                    "\n" +
                    "OX truefalse:\n" +
                    "{\n" +
                    "  \"id\": \"q2\",\n" +
                    "  \"type\": \"truefalse\",\n" +
                    "  \"text\": \"문제 지문\",\n" +
                    "  \"answer\": true 또는 false,\n" +
                    "  \"explanation\": \"문제 풀이 → 정답 도출 → 정답 근거 요약('따라서 정답은 무엇(true, false 중 하나)입니다.' 반드시 포함)\"\n" +
                    "}\n" +
                    "\n" +
                    "주관식 subjective:\n" +
                    "{\n" +
                    "  \"id\": \"q3\",\n" +
                    "  \"type\": \"subjective\",\n" +
                    "  \"text\": \"문제 지문\",\n" +
                    "  \"answer\": \"정답\",\n" +
                    "  \"explanation\": \"문제 풀이 → 정답 도출 → 정답 근거 요약('따라서 정답은 무엇입니다.' 반드시 포함)\"\n" +
                    "}\n" +
                    "\n" +
                    "※ JSON 전체를 배열로 묶어야 함: [..., ..., ...]\n" +
                    "\n" +
                    "[주의]\n" +
                    "- 출제 시 항상 문제(text) → 보기(options) → 정답(answer) → 해설(explanation) 순으로 작성\n" +
                    "- 문제 작성 도중 N개를 넘거나 모자라면 즉시 중단\n" +
                    "- 요청 내용 외 다른 설명은 절대 추가하지 말 것\n" +
                    "보기에 비슷한 답이 있더라도 혼란을 주는 멘트를 해설에 추가하지 마세요.\n" +
                    "\n" +
                    "계산 결과가 확실한 경우, 정답을 명확히 제시하고 다른 보기와 비교하는 멘트를 쓰지 마세요.\n" +
                    "\n";

    @PostMapping("/log")
    public GptResponseDto generateFromQuizLog(@RequestBody QuizLogRequest req) {
        try {
            String prompt = "[대단원] " + req.getChapter() + ", [중단원] " + req.getMiddle() + ", [소단원] " + req.getSmall() + "[문제 개수] " + req.getNumberOfProblems() + "개";
            String rawResponse = gptService.getGptResponse(PROMPT_PREFIX + prompt);
            System.out.println("✅ GPT 응답 원문: " + rawResponse);

            // 🔥 여기서 바로 검증 및 정제
            String validatedResponse = GptResponseValidator.validateAndClean(rawResponse, Integer.parseInt(req.getNumberOfProblems()));
            System.out.println("✅ validatedResponse: " + validatedResponse);

            String questionId = questionService.saveQuestionsFromGptResponse(req.getUserId(), req.getUserType(), req.getChapter(), req.getMiddle(), req.getSmall(), validatedResponse);

            return new GptResponseDto(prompt, validatedResponse, questionId);

        } catch (Exception e) {
            return new GptResponseDto("오류 발생", "❌ 응용 문제 생성 실패: " + e.getMessage(), null);
        }
    }

}
