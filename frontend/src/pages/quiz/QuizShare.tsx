import { useEffect, useState } from "react";
import axios from "axios";
import { useParams, useNavigate, useLocation, Navigate } from "react-router-dom";
import QRCode from "react-qr-code";
import Header from "@_components/header";

export default function QuizShare() {
  const BASE_URL = import.meta.env.VITE_BASE_URL;
  const { roomCode } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const quizRoomUrl = `${window.location.origin}/quiz/play/${roomCode}`;

  const [latestQuizTitle, setLatestQuizTitle] = useState("");
  const [classroom, setClassroom] = useState("");

  const user = JSON.parse(localStorage.getItem("user") || "null");

  // 로그인 안 되어있으면 /login으로 보내고, 로그인 후 돌아올 경로 전달
  if (!user?.userId) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  useEffect(() => {
    const rawParts = roomCode?.split("-") || [];
    const schoolCode = rawParts[1];
    const grade = rawParts[2];
    const className = rawParts[3];

    setClassroom(`${schoolCode}-${grade}-${className}`);

    axios
      .get(`${BASE_URL}/api/quiz/classroom/${schoolCode}/${grade}/${className}`)
      .then((res) => {
        const quizList = res.data;
        if (!quizList || quizList.length === 0) return;

        const last = quizList[quizList.length - 1];
        const parts = last?.questionId?.split("-");
        const batchNum = parts?.[parts.length - 1];
        const title = `${batchNum}. ${last.unit1}-${last.unit2}-${last.unit3}`;
        setLatestQuizTitle(title);
      })
      .catch((err) => {
        console.error("퀴즈 정보 불러오기 실패", err);
      });
  }, [roomCode]);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
          <div className="text-center mb-8">
            <h1 className="text-2xl font-bold mb-4">퀴즈 공유</h1>
            <p className="mb-6 text-gray-600 dark:text-gray-300">
              아래 QR 코드를 스캔하면 문제 풀이 방에 바로 입장할 수 있어요!
            </p>

            <div className="flex justify-center relative mt-8">
              <QRCode value={quizRoomUrl} size={500} />
            </div>

            {latestQuizTitle && (
              <div className="mt-8 w-full text-sm text-gray-600 text-center">
                <p>
                  이 퀴즈는 <strong>우리반 &gt; 생성한 문제 &gt; {latestQuizTitle}</strong>에서 확인 가능합니다.
                </p>
                <button
                  onClick={() => navigate(`/quiz/room/${classroom}`)}
                  className="mt-4 bg-indigo-600 hover:bg-indigo-700 text-white text-sm px-4 py-2 rounded shadow"
                >
                  우리반으로 이동하기
                </button>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
