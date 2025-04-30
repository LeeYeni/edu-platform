import { useEffect, useState } from "react";
import axios from "axios";
import Header from "@_components/header";
import { Link } from "react-router-dom";

interface QuizResult {
  questionId: string;
  questionNum: number;
  correct: boolean;
  userAnswer: string;
  correctAnswer: string;
}

interface CreatedQuestion {
  questionId: string;
  questionNum: number;
  questionText: string;
  explanation: string;
  options: string;
  questionType: string;
  unit1: string;
  unit2: string;
  unit3: string;
}

interface QuizHistoryItem {
  id: string;
  score: number;
  totalQuestions: number;
  correctAnswers: number;
  wrongAnswers: WrongAnswerItem[];
}

interface WrongAnswerItem {
  question: string;
  questionText: string;
  userAnswer: string;
  correctAnswer: string;
  explanation: string;
  options: string[];
  questionType: string | null;
}

export default function MyPage() {
    const BASE_URL = import.meta.env.VITE_BASE_URL;
  const [activeTab, setActiveTab] = useState<string>("report");
  const [expandedQuiz, setExpandedQuiz] = useState<string | null>(null);
  const [quizHistory, setQuizHistory] = useState<QuizHistoryItem[]>([]);
  const [createdQuizData, setCreatedQuizData] = useState<CreatedQuestion[]>([]);

  const user = JSON.parse(localStorage.getItem("user") || "{}");

  useEffect(() => {
    let fetchedResults: QuizResult[] = [];

    axios.get(`${BASE_URL}/api/quiz/results/${user.userId}`)
      .then((res) => {
        fetchedResults = res.data;
        return axios.get(`${BASE_URL}/api/quiz/created/${user.userId}`);
      })
      .then((res) => {
        const createdData: CreatedQuestion[] = res.data;
        setCreatedQuizData(createdData);

        const grouped = fetchedResults.reduce((acc: Record<string, QuizResult[]>, curr) => {
          if (!acc[curr.questionId]) acc[curr.questionId] = [];
          acc[curr.questionId].push(curr);
          return acc;
        }, {});

        const quizHistory: QuizHistoryItem[] = Object.entries(grouped).map(([questionId, entries]) => {
          const totalQuestions = entries.length;
          const correct = entries.filter(e => e.correct).length;

          const wrongAnswers: WrongAnswerItem[] = entries
            .filter(e => !e.correct)
            .map(e => {
              const matchedQuestion = createdData.find(
                q => q.questionId === e.questionId && q.questionNum === e.questionNum
              );
              return {
                question: `${e.questionNum}`,
                questionText: matchedQuestion?.questionText || "문제를 불러올 수 없습니다.",
                userAnswer: e.userAnswer,
                correctAnswer: e.correctAnswer,
                explanation: matchedQuestion?.explanation || "해설을 찾을 수 없습니다.",
                options: matchedQuestion?.options ? JSON.parse(matchedQuestion.options) : [],
                questionType: matchedQuestion?.questionType || null,
              };
            });

          return {
            id: questionId,
            score: Math.round((correct / totalQuestions) * 100),
            totalQuestions,
            correctAnswers: correct,
            wrongAnswers,
          };
        });
        setQuizHistory(quizHistory);
      })
      .catch((err) => {
        console.error("결과 또는 문제 조회 실패", err);
      });
  }, []);

  const uniqueQuizMap = new Map<string, QuizHistoryItem & { title: string }>();
  createdQuizData.forEach((q) => {
    if (!uniqueQuizMap.has(q.questionId)) {
      const record = quizHistory.find(h => h.id === q.questionId);
      const batchNum = parseInt(q.questionId.split("-").pop()!);
      const title = `${batchNum}. ${q.unit1}-${q.unit2}-${q.unit3}`;
      uniqueQuizMap.set(q.questionId, {
        id: q.questionId,
        title,
        score: record?.score ?? 0,
        correctAnswers: record?.correctAnswers ?? 0,
        totalQuestions: record?.totalQuestions ?? 0,
        wrongAnswers: record?.wrongAnswers ?? [],
      });
    }
  });

  const mergedQuizRecords = Array.from(uniqueQuizMap.values());

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex flex-col md:flex-row gap-8">
          <div className="md:w-64">
            <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
              <div className="flex items-center space-x-4 mb-6">
                <div className="h-12 w-12 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
                <div>
                  <h2 className="text-lg font-medium text-gray-900">{user.name} 학생</h2>
                </div>
              </div>
              <nav className="space-y-2">
                <button onClick={() => setActiveTab("report")} className={`block w-full text-left px-4 py-2 rounded-md ${activeTab === "report" ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-700 hover:bg-gray-50"}`}>학습 리포트</button>
                <button onClick={() => setActiveTab("wrong-answers")} className={`block w-full text-left px-4 py-2 rounded-md ${activeTab === "wrong-answers" ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-700 hover:bg-gray-50"}`}>오답 노트</button>
              </nav>
            </div>
          </div>

          <div className="flex-1">
            {activeTab === "report" && (
              <div className="space-y-6">
                <div className="bg-white rounded-lg shadow-sm p-6">
                  <h2 className="text-xl font-bold mb-6">학습 리포트</h2>
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead>
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">퀴즈 제목</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">점수</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">정답/문제</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {mergedQuizRecords.map((quiz) => (
                        <tr key={quiz.id}>
                          <td className="px-6 py-4 text-sm font-medium text-gray-900">{quiz.title}</td>
                          <td className="px-6 py-4 text-sm">
                            {quiz.score !== null ? (
                              <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${quiz.score >= 90 ? "bg-green-100 text-green-800" : quiz.score >= 70 ? "bg-indigo-100 text-indigo-800" : "bg-red-100 text-red-800"}`}>{quiz.score}점</span>
                            ) : (
                              <span className="text-gray-400 text-xs">-</span>
                            )}
                          </td>
                          <td className="px-6 py-4 text-sm text-gray-500">{quiz.correctAnswers ?? "-"}/{quiz.totalQuestions ?? "-"}</td>
                          <td className="px-6 py-4 text-sm text-right">
                            <Link to={`/quiz/play/${quiz.id}`} className="text-indigo-600 hover:text-indigo-900">풀기</Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {activeTab === "wrong-answers" && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <h2 className="text-xl font-bold mb-6">오답 노트</h2>
                {createdQuizData.length === 0 ? (
                  <p className="text-gray-500">생성한 문제가 없습니다.</p>
                ) : (
                  Array.from(new Set(createdQuizData.map(q => q.questionId))).map((qid) => {
                    const quizMeta = createdQuizData.find(q => q.questionId === qid);
                    const title = quizMeta
                      ? `${quizMeta.questionId.split("-").pop()}. ${quizMeta.unit1}-${quizMeta.unit2}-${quizMeta.unit3}`
                      : qid;

                    const isExpanded = expandedQuiz === qid;
                    const quiz = mergedQuizRecords.find(q => q.id === qid);

                    return (
                      <div key={qid} className="border border-gray-200 rounded-lg mb-4">
                        <div
                          className="bg-gray-50 px-4 py-3 flex justify-between items-center cursor-pointer"
                          onClick={() => setExpandedQuiz(isExpanded ? null : qid)}
                        >
                          <div>
                            <h3 className="text-lg font-semibold text-gray-800">{title}</h3>
                          </div>
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className={`h-5 w-5 text-gray-500 transition-transform ${isExpanded ? "rotate-180" : ""}`}
                            viewBox="0 0 20 20"
                            fill="currentColor"
                          >
                            <path
                              fillRule="evenodd"
                              d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                              clipRule="evenodd"
                            />
                          </svg>
                        </div>

                        {isExpanded && (
                          <div className="p-4 space-y-4">
                            {!quiz ? (
                                  <p className="text-gray-500">문제를 풀어주세요.</p>
                                ) : quiz.wrongAnswers.length === 0 ? (
                                  <p className="text-green-600 font-medium">오답이 없습니다. 축하해요!</p>
                                ) : (
                                  quiz.wrongAnswers.map((wrong, index) => (
                                <div key={index} className="bg-gray-50 p-4 rounded">
                                  <p className="font-medium text-gray-900 mb-1">Q{wrong.question}. {wrong.questionText}</p>
                                  {wrong.questionType === "multiple" && Array.isArray(wrong.options) && wrong.options.length > 0 && (
                                    <ul className="mb-3 text-sm text-gray-800 space-y-1">
                                      {wrong.options.map((opt, j) => {
                                        type Parsed = { id: string; text: string };

                                        const parsed: Parsed =
                                          typeof opt === 'string'
                                            ? (opt.replace(/[{}]/g, '')
                                                .split(', ')
                                                .reduce((acc, pair) => {
                                                  const [key, value] = pair.split('=');
                                                  acc[key.trim()] = value.trim();
                                                  return acc;
                                                }, {} as any) as Parsed)  // 👈 여기서 타입 단언
                                            : opt as Parsed;


                                        return (
                                          <li key={j}><span className="font-medium">{parsed.id}.</span> {parsed.text}</li>
                                        );
                                      })}
                                    </ul>
                                  )}
                                  <div className="flex space-x-4 mb-3">
                                    <div className="text-red-600 font-medium">
                                      내 답: <span className="text-gray-700">{wrong.userAnswer === 'true' ? 'O' : wrong.userAnswer === 'false' ? 'X' : wrong.userAnswer}</span>
                                    </div>
                                    <div className="text-green-600 font-medium">
                                      정답: <span className="text-gray-700">{wrong.correctAnswer === 'true' ? 'O' : wrong.correctAnswer === 'false' ? 'X' : wrong.correctAnswer}</span>
                                    </div>
                                  </div>
                                  <div className="bg-blue-50 p-3 rounded text-blue-800 text-sm">
                                    <strong>해설:</strong> {wrong.explanation}
                                  </div>
                                </div>
                              ))
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })
                )}
              </div>
            )}


          </div>
        </div>
      </main>
    </div>
  );
}
