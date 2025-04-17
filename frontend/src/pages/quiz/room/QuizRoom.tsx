import { useEffect, useState } from "react";
import axios from "axios";
import Header from "@_components/header";
import { Link, useParams } from "react-router-dom";
import { Pie, Bar } from 'react-chartjs-2';
// QuizRoom.tsx 상단
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement
} from 'chart.js';

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement);

type Parsed = {
  id: string;
  text: string;
};



interface QuizResultItem {
  questionId: string;
  questionNum: number;
  correct: boolean;
  userAnswer: string;
  correctAnswer: string;
  questionText: string;
  explanation?: string;
  options?: string | Option[];
  [key: string]: any;
}

interface Option {
  id: string;
  text: string;
}

interface CreatedQuizDataItem {
  questionId: string;
  questionNum: number;
  unit1: string;
  unit2: string;
  unit3: string;
  questionText: string;
  explanation: string;
  answer: string;
  options: string;
}

interface Student {
  id: string;
  studentId: string;
  name: string;
}

interface StudentResult {
  userId: string;
  studentId: string;
  name: string;
  results: QuizResultItem[];
}

interface ClassStats {
  total: number;
  submitted: number;
  notSubmittedIds: string[];
  topWrongQuestions: string[];
  reportByQuestionId?: Record<string, {
    total: number;
    submitted: number;
    notSubmittedIds: string[];
    topWrongQuestions: string[];
  }>;
}

interface User {
  userId: string;
  name: string;
  userType: "teacher" | "student";
}

interface MergedQuizRecord {
  id: string;
  title: string;
  score: number | null;
  correctAnswers: number | null;
  totalQuestions: number | null;
  wrongAnswers: WrongAnswer[];
}

interface WrongAnswer {
  question: string;
  questionText: string;
  userAnswer: string;
  correctAnswer: string;
  explanation: string;
  options: Option[];
}

export default function QuizRoom() {
   const roomCode = useParams().roomCode!;
     const [activeTab, setActiveTab] = useState<string>("report");
     const [expandedQuiz, setExpandedQuiz] = useState<string | null>(null)
     const [expandedQuestionId, setExpandedQuestionId] = useState<string | null>(null);
     const [quizHistory, setQuizHistory] = useState<any[]>([]);
     const [createdQuizData, setCreatedQuizData] = useState<CreatedQuizDataItem[]>([]);
     const [studentReportData, setStudentReportData] = useState<StudentResult[]>([]);
     const [studentList, setStudentList] = useState<Student[]>([]);
     const [expandedQid, setExpandedQid] = useState<string | null>(null);
     const [classStats, setClassStats] = useState<ClassStats>({
       total: 0,
       submitted: 0,
       notSubmittedIds: [],
       topWrongQuestions: [],
     });

     const user: User = JSON.parse(localStorage.getItem("user") || "{}");

  useEffect(() => {
      let fetchedResults: QuizResultItem[] = [];

      axios.get(`/api/quiz/results/${user.userId}`)
        .then((res) => {
          fetchedResults = res.data;
          const [schoolCode, grade, className] = roomCode.split("-");
          return axios.get(`/api/quiz/classroom/${schoolCode}/${grade}/${className}`);
        })
        .then((res) => {
          const createdData: CreatedQuizDataItem[] = res.data;
          setCreatedQuizData(createdData);

          const grouped = fetchedResults.reduce<Record<string, QuizResultItem[]>>((acc, curr) => {
            if (!acc[curr.questionId]) acc[curr.questionId] = [];
            acc[curr.questionId].push(curr);
            return acc;
          }, {});

          const history = Object.entries(grouped).map(([questionId, entries]) => {
            const totalQuestions = entries.length;
            const correct = entries.filter(e => e.correct).length;

            const wrongAnswers = entries
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

          setQuizHistory(history);

          if (user.userType === "teacher") {
            axios.get(`/api/quiz/students/${roomCode}`).then(res => setStudentReportData(res.data));
            axios.get(`/api/user/classroom/${roomCode}`).then(res => setStudentList(res.data));
            axios.get(`/api/report/classroom/${roomCode}`).then(res => setClassStats(res.data));
          }
        })
        .catch(err => console.error("데이터 로딩 실패", err));
    }, [roomCode]);

  const uniqueQuizMap = new Map();
  createdQuizData.forEach((q) => {
    if (!uniqueQuizMap.has(q.questionId)) {
      const record = quizHistory.find(h => h.id === q.questionId);
      const batchNum = parseInt(q.questionId.split("-").pop() || "0");
      const title = `${batchNum}. ${q.unit1}-${q.unit2}-${q.unit3}`;
      uniqueQuizMap.set(q.questionId, {
        id: q.questionId,
        title,
        score: record?.score ?? null,
        correctAnswers: record?.correctAnswers ?? null,
        totalQuestions: record?.totalQuestions ?? null,
        wrongAnswers: record?.wrongAnswers ?? [],
      });
    }
  });

  const groupedByQuestionId: Record<string, any[]> = studentReportData.reduce((acc: Record<string, any[]>, curr) => {
    curr.results.forEach((res: any) => {
      if (!acc[res.questionId]) acc[res.questionId] = [];
      acc[res.questionId].push({
        ...res,
        userName: curr.name,
        userId: curr.userId,
        studentId: curr.studentId,
      });
    });
    return acc;
  }, {});

  const mergedQuizRecords: MergedQuizRecord[] = Array.from(
      new Map(
        createdQuizData.map((q) => {
          const record = quizHistory.find((h) => h.id === q.questionId);
          const batchNum = parseInt(q.questionId.split("-").pop() || "0");
          const title = `${batchNum}. ${q.unit1}-${q.unit2}-${q.unit3}`;
          return [q.questionId, {
            id: q.questionId,
            title,
            score: record?.score ?? null,
            correctAnswers: record?.correctAnswers ?? null,
            totalQuestions: record?.totalQuestions ?? null,
            wrongAnswers: record?.wrongAnswers ?? [],
          }];
        })
      ).values()
    );

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
                  <h2 className="text-lg font-medium text-gray-900">{user.name} {user.userType === "teacher" ? "선생님" : "학생"}</h2>
                </div>
              </div>
              <nav className="space-y-2">
              <button onClick={() => setActiveTab("report")} className={`block w-full text-left px-4 py-2 rounded-md ${activeTab === "report" ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-700 hover:bg-gray-50"}`}>{user.userType === "teacher" ? "생성한 문제": "학습 리포트"}</button>
                {user.userType === "student" && (
                    <button onClick={() => setActiveTab("wrong-answers")} className={`block w-full text-left px-4 py-2 rounded-md ${activeTab === "wrong-answers" ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-700 hover:bg-gray-50"}`}>오답 노트</button>
                )}
                {user.userType === "teacher" && (
                    <>
                    <button onClick={() => setActiveTab("class-report")} className={`block w-full text-left px-4 py-2 rounded-md ${activeTab === "class-report" ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-700 hover:bg-gray-50"}`}>학급 리포트</button>
                  <button onClick={() => setActiveTab("student-report")} className={`block w-full text-left px-4 py-2 rounded-md ${activeTab === "student-report" ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-700 hover:bg-gray-50"}`}>개별 리포트</button>
                  <button onClick={() => setActiveTab("student-info")} className={`block w-full text-left px-4 py-2 rounded-md ${activeTab === "student-info" ? "bg-indigo-50 text-indigo-700 font-medium" : "text-gray-700 hover:bg-gray-50"}`}>우리반 학생 정보</button>
                  </>
                )}
              </nav>
            </div>
          </div>

          <div className="flex-1">
            {activeTab === "report" && (
              <div className="space-y-6">
                <div className="bg-white rounded-lg shadow-sm p-6">
                  <h2 className="text-xl font-bold mb-6">{user.userType === "teacher" ? "생성한 문제": "학습 리포트"}</h2>
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead>
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">퀴즈 제목</th>
                        {user.userType === "student" && (
                            <>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">점수</th>
                                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">정답/문제</th>
                            </>
                            )}
                        <th></th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {mergedQuizRecords.map((quiz) => (
                        <tr key={quiz.id}>
                          <td className="px-6 py-4 text-sm font-medium text-gray-900">{quiz.title}</td>
                          {user.userType === "student" && (
                              <>
                              <td className="px-6 py-4 text-sm">
                                                          {quiz.score !== null ? (
                                                            <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${quiz.score >= 90 ? "bg-green-100 text-green-800" : quiz.score >= 70 ? "bg-indigo-100 text-indigo-800" : "bg-red-100 text-red-800"}`}>{quiz.score}점</span>
                                                          ) : (
                                                            <span className="text-gray-400 text-xs">-</span>
                                                          )}
                                                        </td>
                                                        <td className="px-6 py-4 text-sm text-gray-500">{quiz.correctAnswers ?? "-"}/{quiz.totalQuestions ?? "-"}</td>
                              </>
                              )}
                          <td className="px-6 py-4 text-sm text-right">
                            <Link to={`/quiz/play/${quiz.id}`} className="text-indigo-600 hover:text-indigo-900">{user.userType === "teacher" ? "문제 확인" : "풀기"}</Link>
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
    {mergedQuizRecords.map((quiz) => {
      const isExpanded = expandedQuiz === quiz.id;
      return (
        <div key={quiz.id} className="mb-6 border border-gray-200 rounded">
          <div
            className="bg-gray-100 px-4 py-3 lex justify-between items-center cursor-pointer"
            onClick={() => setExpandedQuiz(isExpanded ? null : quiz.id)}
          >
            <h3 className="text-lg font-semibold text-gray-800">{quiz.title}</h3>
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
  <div className="p-4">
      {!quiz.wrongAnswers[0]?.userAnswer ? (
        <p className="text-gray-500">문제를 풀어주세요.</p>
      ) : quiz.wrongAnswers.length === 0 ? (
        <p className="text-green-600 font-medium">오답이 없습니다. 축하해요!</p>
      ) : (
      quiz.wrongAnswers.map((wrong: any, i: number) => (
        <div key={i} className="bg-gray-50 p-4 rounded border border-gray-200 mb-4">
          <p className="font-medium text-gray-900 mb-1">Q{wrong.question}. {wrong.questionText}</p>
          {Array.isArray(wrong.options) && wrong.options.length > 0 && (
            <ul className="mb-3 text-sm text-gray-800 space-y-1">
              {wrong.options.map((opt: any, j: number) => {
                const parsed: Parsed = typeof opt === 'string'
                  ? opt.replace(/[{}]/g, '')
                      .split(', ')
                      .reduce((acc: Record<string, string>, pair: string) => {
                        const [key, value] = pair.split('=');
                        acc[key.trim()] = value.trim();
                        return acc;
                      }, {} as Record<string, string>) as Parsed
                  : opt as Parsed;
                return (
                  <li key={j}><span className="font-medium">{parsed.id}.</span> {parsed.text}</li>
                );
              })}
            </ul>
          )}
          <div className="text-sm space-y-1">
            <div className="text-red-600 font-medium">
              오답:{" "}
              <span className="text-gray-700">
                {wrong.userAnswer === "true"
                  ? "O"
                  : wrong.userAnswer === "false"
                  ? "X"
                  : wrong.userAnswer}
              </span>
            </div>
            <div className="text-green-600 font-medium">
              정답:{" "}
              <span className="text-gray-700">
                {wrong.correctAnswer === "true"
                  ? "O"
                  : wrong.correctAnswer === "false"
                  ? "X"
                  : wrong.correctAnswer}
              </span>
            </div>
            <div className="bg-blue-50 p-3 rounded text-blue-800 text-sm mt-2">
              <strong>해설:</strong> {wrong.explanation}
            </div>
          </div>
        </div>
      ))
    )}
  </div>
)}


        </div>
      );
    })}
  </div>
)}



{activeTab === "class-report" && (
  <div className="bg-white rounded-lg shadow-sm p-6">
    <h2 className="text-xl font-bold mb-6">학급 리포트</h2>
    {Array.from(new Set(createdQuizData.map(q => q.questionId))).map((qid) => {
      const report = classStats.reportByQuestionId?.[qid];
      const quizMeta = createdQuizData.find(q => q.questionId === qid);
      const quizTitle = quizMeta
        ? `${quizMeta.questionId.split("-").pop()}. ${quizMeta.unit1}-${quizMeta.unit2}-${quizMeta.unit3}`
        : qid;

      const submitted = report?.submitted || 0;
      const total = report?.total || studentList.length || 1;
      const notSubmittedIds = report?.notSubmittedIds || [];

      const entries = studentReportData
        .flatMap(student => student.results)
        .filter(r => r.questionId === qid);

      const questionMap = createdQuizData
        .filter(q => q.questionId === qid)
        .reduce((acc, q) => {
          acc[q.questionNum] = q;
          return acc;
        }, {} as Record<number, any>);

      const groupedByQuestionNum = entries.reduce((acc, curr) => {
        const qnum = curr.questionNum;
        if (!acc[qnum]) acc[qnum] = [];
        acc[qnum].push(curr);
        return acc;
      }, {} as Record<number, any[]>);

      return (
        <div key={qid} className="border border-gray-200 rounded-lg mb-6">
          <div
            className="bg-gray-100 px-4 py-3 flex justify-between items-center cursor-pointer"
            onClick={() => setExpandedQid(expandedQid === qid ? null : qid)}
          >
            <h3 className="text-lg font-semibold">{quizTitle}</h3>
            <svg xmlns="http://www.w3.org/2000/svg" className={`h-5 w-5 text-gray-500 transition-transform ${expandedQid === qid ? "rotate-180" : ""}`} viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </div>

          {expandedQid === qid && (
            <div className="p-4">
              <h4 className="text-md font-semibold mb-2">퀴즈 제출률</h4>
              <div style={{ width: "300px", height: "300px" }}>
                <Pie
                  data={{
                    labels: ["제출", "미제출"],
                    datasets: [{
                      label: "제출률",
                      data: [submitted, total - submitted],
                      backgroundColor: ["#4F46E5", "#E5E7EB"],
                      borderWidth: 1,
                    }]
                  }}
                  options={{ maintainAspectRatio: false }}
                />
              </div>
              <p className="mt-2 text-sm text-gray-600">제출 학생: {submitted} / 전체: {total}</p>
              {notSubmittedIds.length > 0 && (
                <p className="text-sm font-medium text-red-600 mt-1">미제출 학생 번호: {notSubmittedIds.join(", ")}</p>
              )}

              <h4 className="text-md font-semibold mt-6 mb-4">문항별 응답</h4>
              {Object.entries(groupedByQuestionNum).map(([num, responses]) => {
                const q = questionMap[parseInt(num)];
                const totalCount = responses.length;
                const correctAnswer = q.answer?.replace(/"/g, "").trim();
                const parsedOptions = q.options ? JSON.parse(q.options) : [];

                const answerCounts = responses.reduce((acc, r) => {
                  const ans = r.userAnswer?.replace(/"/g, "").trim();
                  acc[ans] = (acc[ans] || 0) + 1;
                  return acc;
                }, {} as Record<string, number>);

                let labels: string[] = [];

                const isTrueFalse = (correctAnswer === "true" || correctAnswer === "false") && parsedOptions.length === 0;

                if (parsedOptions.length >= 2 && parsedOptions.length <= 4) {
                  labels = parsedOptions.map((opt: any) => opt.id);
                } else if (isTrueFalse) {
                  labels = ["O", "X"];
                } else {
                  const topAnswers = Object.entries(answerCounts as Record<string, number>)
                    .sort((a, b) => b[1] - a[1])
                    .slice(0, 4);
                  labels = topAnswers.map(([ans]) => ans);
                }

                const data = labels.map(id => {
                  const mappedId = id === "O" ? "true" : id === "X" ? "false" : id;
                  return {
                    id,
                    percentage: totalCount ? (answerCounts[mappedId] || 0) * 100 / totalCount : 0
                  };
                });

                return (
                  <div key={num} className="border border-gray-300 rounded-lg p-4 mb-4">
                    <h5 className="font-bold mb-2">Q{q.questionNum}. {q.questionText}</h5>
                    {parsedOptions.length >= 2 && parsedOptions.length <= 4 && (
                      <ul className="mb-3 text-sm">
                        {parsedOptions.map((opt: any, j: number) => (
                          <li key={j}>
                            <span className="font-medium">{opt.id}.</span> {opt.text}
                          </li>
                        ))}
                      </ul>
                    )}
                    <h5 className="font-bold mb-2">정답: {correctAnswer === "true" ? "O" : correctAnswer === "false" ? "X" : correctAnswer}</h5>
                    <div className="w-full sm:w-2/3">
                      <Bar
                        data={{
                          labels: labels,
                          datasets: [{
                            label: '응답 비율 (%)',
                            data: data.map(d => d.percentage),
                            backgroundColor: data.map(d =>
                              (d.id === (correctAnswer === "true" ? "O" : correctAnswer === "false" ? "X" : correctAnswer)) ? '#3B82F6' : '#EF4444'
                            ),
                            borderWidth: 1,
                          }]
                        }}
                        options={{
                          indexAxis: 'y',
                          scales: {
                            x: {
                              beginAtZero: true,
                              max: 100,
                              ticks: {
                                callback: value => value + '%',
                              },
                            },
                          },
                          plugins: {
                            legend: { display: false },
                          },
                        }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      );
    })}
  </div>
)}






{activeTab === "student-report" && (
  <div className="bg-white rounded-lg shadow-sm p-6">
    <h2 className="text-xl font-bold mb-6">개별 리포트</h2>

    {createdQuizData.length === 0 ? (
      <p className="text-gray-500">아직 생성된 퀴즈가 없습니다.</p>
    ) : (
      Array.from(new Set(createdQuizData.map(q => q.questionId))).map((qid) => {
        const quizMeta = createdQuizData.find(q => q.questionId === qid);
        const questionTitle = quizMeta
                ? `${quizMeta.questionId.split("-").pop()}. ${quizMeta.unit1}-${quizMeta.unit2}-${quizMeta.unit3}`
                : qid;

        const entries = groupedByQuestionId[qid] || [];

        // 1. 오답 학생 데이터 모으기
        const wrongByUser = entries.reduce((acc, cur) => {
          if (!acc[cur.userId]) {
            acc[cur.userId] = { userName: cur.userName, studentId: cur.studentId, answers: [] };
          }
          if (!cur.correct) {
            acc[cur.userId].answers.push(cur);
          }
          return acc;
        }, {});

        // 2. 이 반 전체 학생 기준으로 박스 구성
        const boxes = studentList
          .sort((a, b) => parseInt(a.studentId) - parseInt(b.studentId))
          .map((stu) => {
            const match = wrongByUser[stu.id];
            return {
              userId: stu.id,
              studentId: stu.studentId,
              userName: stu.name,
              answers: match?.answers || [],
            };
          });

        return (
          <div key={qid} className="border border-gray-200 rounded-lg mb-6">
            <div
              className="bg-gray-100 px-4 py-3 flex justify-between items-center cursor-pointer"
              onClick={() => setExpandedQuestionId(expandedQuestionId === qid ? null : qid)}
            >
              <h3 className="text-lg font-semibold">{questionTitle}</h3>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className={`h-5 w-5 text-gray-500 transition-transform ${expandedQuestionId === qid ? "transform rotate-180" : ""}`}
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

            {expandedQuestionId === qid && (
              <div className="px-5 py-4 space-y-6 bg-white rounded-b-lg">
                {entries.length === 0 ? (
                  <p className="text-sm text-gray-500">아직 문제를 푼 학생이 없습니다.</p>
                ) : (
                  boxes.map(({ userId, studentId, userName, answers }) => (
                    <div key={userId} className="bg-gray-50 p-4 rounded border border-gray-200">
                      <p className="font-semibold text-gray-800 mb-2">{studentId}. {userName}</p>
                      {answers.length === 0 ? (
                        entries.some(e => e.userId === userId) ? (
                          <p className="text-sm text-gray-500">오답이 없습니다.</p>
                        ) : (
                          <p className="text-sm text-gray-400">제출 기록이 없습니다.</p>
                        )
                      ) : (
                        answers.map((r: any, i: number) => (
                          <div key={i} className="mb-4 border-b border-gray-200 pb-4">
                            <p className="font-medium text-gray-900 mb-2">Q{r.questionNum}. {r.questionText}</p>
                            {r.options && r.options.length > 0 && (
                              <ul className="mb-3 text-sm text-gray-800 space-y-1">
                                {r.options.map((opt: any, j: number) => {
                                  const parsed = opt.replace(/[{}]/g, '')
                                    .split(', ')
                                    .reduce((acc: Record<string, string>, pair: string) => {
                                      const [key, value] = pair.split('=');
                                      acc[key.trim()] = value.trim();
                                      return acc;
                                    }, {});
                                  return (
                                    <li key={j}>
                                      <span className="font-medium">{parsed.id}.</span> {parsed.text}
                                    </li>
                                  );
                                })}
                              </ul>
                            )}
                            <div className="text-sm space-y-1">
                              <div className="text-red-600 font-medium">오답: <span className="text-gray-700">{r.userAnswer}</span></div>
                              <div className="text-green-600 font-medium">정답: <span className="text-gray-700">{r.correctAnswer}</span></div>
                            </div>
                          </div>
                        ))
                      )}
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






{activeTab === "student-info" && (
                <div className="bg-white rounded-lg shadow-sm p-6">
                  <h2 className="text-xl font-bold mb-6">우리반 학생 정보</h2>
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead>
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">번호</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">이름</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">아이디</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {studentList
                        .sort((a, b) => parseInt(a.studentId) - parseInt(b.studentId))
                        .map((stu, idx) => (
                          <tr key={idx}>
                            <td className="px-6 py-4 text-sm text-gray-900">{stu.studentId}</td>
                            <td className="px-6 py-4 text-sm text-gray-900">{stu.name}</td>
                            <td className="px-6 py-4 text-sm text-gray-500">{stu.id}</td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              )}

          </div>
        </div>
      </main>
    </div>
  );
}

