import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import Header from "@_components/header";

export default function QuizPlay() {
  const { roomCode } = useParams();
  const router = useNavigate();

  const [quiz, setQuiz] = useState<any>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState<any>(null);
  const [isAnswered, setIsAnswered] = useState(false);
  const [showExplanation, setShowExplanation] = useState(false);
  const [userAnswers, setUserAnswers] = useState<any[]>([]);
  const [quizCompleted, setQuizCompleted] = useState(false);
  const [feedbackVisible, setFeedbackVisible] = useState(false);
  const [isCorrect, setIsCorrect] = useState<boolean | null>(null);
  const [alreadySolved, setAlreadySolved] = useState(false);

  const user = JSON.parse(localStorage.getItem("user") || "{}");

  if (!roomCode || (!roomCode.startsWith("s-") && !roomCode.startsWith("t-"))) {
    return <p>잘못된 방 코드입니다.</p>;
  }


  useEffect(() => {
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    axios.get(`/api/quiz/result/exists`, {
      params: {
        userId: user.userId,
        questionId: roomCode,
      }
    }).then((res) => {
      setAlreadySolved(res.data);  // true or false
      console.log(res.data);
    });
  }, [roomCode]);

  useEffect(() => {
    if (roomCode) {
      axios.get(`/api/quiz/play/${roomCode}`)
        .then((res) => {
          const questions = res.data.map((q: any) => ({
            id: q.id,
            questionId: q.questionId,
            number: q.questionNum,
            type: q.questionType,
            text: q.questionText,
            options: q.options ? JSON.parse(q.options) : null,
            answer: q.questionType === "truefalse"
              ? q.answer === "true"
              : q.answer.replaceAll('"', ''),
            explanation: q.explanation
          }));

          setQuiz({ questions });
          setUserAnswers(new Array(questions.length).fill(null));
        })
        .catch((err) => {
          console.error("퀴즈 불러오기 실패:", err);
          alert("문제를 불러오는 데 실패했어요. 🥲");
        });
    }
  }, [roomCode]);

  const handleAnswerSelect = (answer: any) => {
    if (isAnswered) return;
    const current = quiz.questions[currentQuestionIndex];
    const correct = answer === current.answer;
    setSelectedAnswer(answer);
    setIsAnswered(true);
    setIsCorrect(correct);
    setFeedbackVisible(true);

    const newUserAnswers = [...userAnswers];
    newUserAnswers[currentQuestionIndex] = answer;
    setUserAnswers(newUserAnswers);

    setTimeout(() => setFeedbackVisible(false), 1000);
  };

  const handleSubmit = () => {
    const user = JSON.parse(localStorage.getItem("user") || "{}");

    // s-와 t-로 시작하지 않으면 저장하지 않음
      if (!roomCode.startsWith("s-") && !roomCode.startsWith("t-")) {
        setQuizCompleted(true);
        return;
      }

    const results = quiz.questions.map((q: any, i: number) => {
      const userAnswer = String(userAnswers[i]);
      const correctAnswer = String(q.answer);
      const isCorrect = userAnswer === correctAnswer;

      return {
        questionNum: q.number,
        userAnswer,
        correctAnswer,
        isCorrect
      };
    });

    const payload = {
      userId: user.userId,
      questionId: roomCode,
      results
    };

    // s-인 경우에는 update 지원
      if (roomCode.startsWith("s-")) {
        const url = alreadySolved ? "/api/quiz/updateResult" : "/api/quiz/saveResult";
        axios[alreadySolved ? "put" : "post"](url, payload)
          .then(() => {
            setQuizCompleted(true);
          })
          .catch((err) => {
            console.error("저장/업데이트 실패:", err);
            setQuizCompleted(true);
          });
      }

      // t-인 경우는 저장만 1회 허용
      else if (roomCode.startsWith("t-") && !alreadySolved) {
        axios.post("/api/quiz/saveResult", payload)
          .then(() => {
            setQuizCompleted(true);
          })
          .catch((err) => {
            console.error("저장 실패:", err);
            setQuizCompleted(true);
          });
      } else {
        setQuizCompleted(true); // t-인데 이미 저장된 경우는 무시
      }
    };



  const calculateScore = () => {
    return quiz
      ? Math.round(
          (quiz.questions.filter((q: any, i: number) => q.answer === userAnswers[i]).length / quiz.questions.length) * 100
        )
      : 0;
  };

  if (!quiz) return null;

  const current = quiz.questions[currentQuestionIndex];

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && selectedAnswer !== null && !isAnswered) {
      handleAnswerSelect(selectedAnswer);
    }
  };

  if (quizCompleted) {
    const score = calculateScore();

    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 overflow-y-auto">
        <Header />
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <h1 className="text-2xl font-bold mb-4">퀴즈 결과</h1>
          <div className="mb-4 text-xl">정답률: {score}%</div>
          {quiz.questions.map((q: any, i: number) =>
            userAnswers[i] !== q.answer ? (
              <div key={q.id} className="bg-gray-100 p-4 rounded mb-6">
                <p className="font-semibold mb-2 whitespace-pre-line">{i + 1}. {q.text}</p>
                {q.type === "multiple" && (
                  <ul className="list-disc list-inside mb-2 space-y-1">
                    {q.options.map((opt: any) => (
                      <li
                        key={opt.id}
                        className={`pl-2 rounded px-1 py-0.5 inline-block
                          ${opt.id === q.answer ? "bg-green-100 text-green-700" : ""}
                          ${userAnswers[i] === opt.id && opt.id !== q.answer ? "bg-red-100 text-red-700" : ""}`}
                      >
                        <strong>{opt.id}.</strong> {opt.text}
                      </li>
                    ))}
                  </ul>
                )}
                <p className="text-sm text-gray-700 whitespace-pre-line">해설: {q.explanation}</p>
              </div>
            ) : null
          )}
          <div className="mt-4 flex justify-between">
            <button className="btn-primary" onClick={() => window.location.reload()}>
              다시 풀기
            </button>
            {roomCode.startsWith("t-") && (
              <button
                className="btn-primary"
                onClick={() => {
                  const parts = roomCode.split("-");
                  const classroomCode = parts.slice(1, 4).join("-");
                  console.log(classroomCode);
                  router(`/quiz/room/${classroomCode}`);
                }}
              >
                우리반으로 이동하기
              </button>
            )}

            {roomCode.startsWith("s-") && (
              <button
                className="btn-primary"
                onClick={() => router("/mypage")}
              >
                마이페이지로 이동하기
              </button>
            )}
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="relative bg-white shadow rounded p-6">
          {feedbackVisible && isCorrect !== null && (
            <div className="absolute top-16 left-1/2 transform -translate-x-1/2 pointer-events-none">
              <div className={`text-9xl ${isCorrect ? "text-green-500" : "text-red-500"} animate-pulse`}>
                {isCorrect ? "O" : "X"}
              </div>
            </div>
          )}
          <h1 className="text-xl font-bold mb-4">문제 {currentQuestionIndex + 1}</h1>
          <div className="text-gray-600 mb-4">{currentQuestionIndex + 1} / {quiz.questions.length}</div>
          <div className="mb-4 text-lg font-medium whitespace-pre-line">{current.text}</div>

          {current.type === "multiple" && (
            <div className="space-y-3">
              {current.options.map((opt: any) => (
                <button
                  key={opt.id}
                  onClick={() => handleAnswerSelect(opt.id)}
                  disabled={isAnswered}
                  className={`block w-full text-left p-3 border rounded ${
                    isAnswered
                      ? opt.id === current.answer
                        ? "bg-green-100 border-green-400"
                        : selectedAnswer === opt.id
                          ? "bg-red-100 border-red-400"
                          : "border-gray-300"
                      : selectedAnswer === opt.id
                        ? "bg-blue-100 border-blue-400"
                        : "border-gray-300"
                  }`}
                >
                  <span className="mr-2 font-bold">{opt.id}.</span> {opt.text}
                </button>
              ))}
            </div>
          )}

          {current.type === "truefalse" && (
            <div className="flex gap-4">
              {["참", "거짓"].map((label, idx) => (
                <button
                  key={label}
                  onClick={() => handleAnswerSelect(idx === 0)}
                  disabled={isAnswered}
                  className={`flex-1 p-3 border rounded ${
                    selectedAnswer === (idx === 0)
                      ? isAnswered && (idx === 0) === current.answer
                        ? "bg-green-100 border-green-400"
                        : isAnswered
                          ? "bg-red-100 border-red-400"
                          : "bg-blue-100 border-blue-400"
                      : "border-gray-300"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          )}

          {current.type === "subjective" && !isAnswered && (
            <div className="mt-2">
              <input
                type="text"
                className="input-field w-full"
                placeholder="답을 입력하세요"
                onChange={(e) => setSelectedAnswer(e.target.value)}
                onKeyDown={handleKeyDown}
              />
            </div>
          )}

          {isAnswered && (
            <div className="mt-4 flex justify-between">
              <button onClick={() => setShowExplanation(!showExplanation)} className="btn-outline">
                {showExplanation ? "해설 숨기기" : "해설 보기"}
              </button>
              {user.userType === "student" ? (
                <button
                  onClick={
                    currentQuestionIndex < quiz.questions.length - 1
                      ? () => {
                          setCurrentQuestionIndex(currentQuestionIndex + 1);
                          setSelectedAnswer(null);
                          setIsAnswered(false);
                          setShowExplanation(false);
                          setIsCorrect(null);
                        }
                      : handleSubmit
                  }
                  className="btn-primary"
                >
                  {currentQuestionIndex < quiz.questions.length - 1 ? "다음 문제" : "제출"}
                </button>
              ) : (
                <button
                  onClick={
                    currentQuestionIndex < quiz.questions.length - 1
                      ? () => {
                          setCurrentQuestionIndex(currentQuestionIndex + 1);
                          setSelectedAnswer(null);
                          setIsAnswered(false);
                          setShowExplanation(false);
                          setIsCorrect(null);
                        }
                      : () => {
                          const parts = roomCode.split("-");
                          const classroomCode = parts.slice(1, 4).join("-");
                          console.log(classroomCode);
                          router(`/quiz/room/${classroomCode}`);
                        }
                  }
                  className="btn-primary"
                >
                  {currentQuestionIndex < quiz.questions.length - 1 ? "다음 문제" : "우리반으로 이동하기"}
                </button>
              )}


            </div>
          )}

          {isAnswered && showExplanation && (
            <div className="mt-4 text-sm bg-blue-50 border border-blue-200 p-3 rounded whitespace-pre-line">
              <strong>해설:</strong> {current.explanation}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
