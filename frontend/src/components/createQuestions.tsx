import { useEffect, useState } from "react";

// GptResponseDto 타입 정의
interface GptResponseDto {
  response: string;
}

interface QuestionData {
  question: string;
  answer: string;
  wrong_choice1: string;
  wrong_choice2: string;
  wrong_choice3: string;
  explanation: string;
}

const QuizComponent = () => {
  const [questionData, setQuestionData] = useState<QuestionData | null>(null);

  // 백엔드에서 데이터를 가져오는 useEffect
  useEffect(() => {
    // API 호출 (자신의 백엔드 API URL로 변경)
    fetch("/from-aihub")
      .then((response) => response.json()) // 백엔드에서 JSON 형식으로 응답을 받음
      .then((data: GptResponseDto) => {
        // 응답에서 JSON 문자열을 파싱하여 각 항목을 분리
        const parsedData = JSON.parse(data.response);

        // 파싱한 데이터를 상태로 설정
        setQuestionData(parsedData);
      })
      .catch((error) => {
        console.error("Error fetching the data:", error);
      });
  }, []);

  if (!questionData) {
    return <div>Loading...</div>;
  }

  return (
    <div>
      <h1>문제: {questionData.question}</h1>
      <div>
        <p>정답: {questionData.answer}</p>
        <p>오답 1: {questionData.wrong_choice1}</p>
        <p>오답 2: {questionData.wrong_choice2}</p>
        <p>오답 3: {questionData.wrong_choice3}</p>
      </div>
      <p>해설: {questionData.explanation}</p>
    </div>
  );
};

export default QuizComponent;
