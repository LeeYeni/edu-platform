import { useState } from "react"
import { useNavigate } from "react-router-dom"
import Header from "@_components/header"
import { Sparkles, QrCode, BarChart3 } from "lucide-react"

export default function Home() {
  const router = useNavigate()
  const BASE_URL = import.meta.env.VITE_BASE_URL;

  const [selectedSchool, setSelectedSchool] = useState("")
  const [selectedGrade, setSelectedGrade] = useState("")
  const [selectedSubject, setSelectedSubject] = useState("")
  const [selectedChapter, setSelectedChapter] = useState("")
  const [selectedMiddle, setSelectedMiddle] = useState("")
  const [selectedSmall, setSelectedSmall] = useState("")
  const [selectedNumberOfProblems, setSelectedNumberOfProblems] = useState("")

  const [isLoading, setIsLoading] = useState(false);

  const schools = ["초등학교"]
  const grades = {
    초등학교: ["3학년"]
  }
  const subjects = {
    초등학교: ["수학"]
  }
  const numberOfProblems = ["5", "10", "15", "20"]

  const mockChaptersByGrade: Record<string, Record<string, Record<string, string[]>>> = {
    "3학년": {
      "수와 연산": {
        "덧셈과 뺄셈": ["(세 자리 수)+(세 자리 수)", "(세 자리 수) - (세 자리 수)", "덧셈과 뺄셈 상황에서의 어림셈"],
        "곱셈": ["(두 자리 수)x(한 자리 수)", "(세 자리 수)x(한 자리 수)", "곱하는 수가 두 자리 수인 곱셈", "곱셈 상황에서 어림셈"],
        "나눗셈": ["나눗셈식", "나눗셈의 몫과 나머지", "(두 자리 수)÷(한 자리 수)", "(세 자리 수)÷(한 자리 수)", "나눗셈 상황에서 어림셈"],
        "분수와 소수": ["분수", "소수"],
      },
    }
  } as const

 const getChapters = () => {
     if (!selectedGrade || !selectedSubject) return []
     const chapters = mockChaptersByGrade[selectedGrade as keyof typeof mockChaptersByGrade]
     return chapters ? Object.keys(chapters) : []
   }

   const getMiddleChapters = () => {
     if (!selectedGrade || !selectedChapter) return []
     const chapter = mockChaptersByGrade[selectedGrade as keyof typeof mockChaptersByGrade]?.[selectedChapter as keyof typeof mockChaptersByGrade[typeof selectedGrade]]
     return chapter ? Object.keys(chapter) : []
   }

   const getSmallChapters = (middleUnit: string) => {
     if (!selectedGrade || !selectedChapter || !middleUnit) return []
     return (
       mockChaptersByGrade[selectedGrade as keyof typeof mockChaptersByGrade]?.[selectedChapter as keyof typeof mockChaptersByGrade[typeof selectedGrade]]?.[middleUnit as keyof typeof mockChaptersByGrade[typeof selectedGrade][typeof selectedChapter]] || []
     )
   }

  const isLoggedIn = () => {
      const user = JSON.parse(localStorage.getItem("user") || "null")
      return !!user?.userId
    }

  const handleGenerateQuiz = async () => {
    if (!isLoggedIn()) {
      alert("로그인 먼저 해주세요.");
      return;
    }

    const user = JSON.parse(localStorage.getItem("user") || "{}");

    if (selectedSchool && selectedGrade && selectedSubject && selectedChapter && selectedMiddle &&
      selectedSmall && selectedNumberOfProblems) {
      const data = {
        school: selectedSchool,
        grade: selectedGrade,
        subject: selectedSubject,
        chapter: selectedChapter,
        middle: selectedMiddle,
        small: selectedSmall,
        numberOfProblems: selectedNumberOfProblems,
        userType: user.userType,
        userId: user.userId,
      };

      try {
        setIsLoading(true); // 🔥 문제 생성 시작할 때 로딩 true

        const response = await fetch(`${BASE_URL}/api/quiz/log`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(data),
        });

        if (response.ok) {
          const result = await response.json();
          const questionId = result.questionId;
          const isStudent = user.userType === "student";

          if (isStudent) {
            router(`/quiz/play/${questionId}`);
          } else {
            router(`/quiz/share/${questionId}`);
          }
        } else {
          alert("문제 생성에 실패했습니다.");
        }
      } catch (error) {
        alert("서버와의 통신 중 오류가 발생했습니다.");
        console.error("❌ 오류:", error);
      } finally {
        setIsLoading(false); // ✅ 성공하든 실패하든 로딩 끝나면 false
      }
    } else {
      alert("모든 항목을 입력해주세요.");
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-grow flex flex-col items-center justify-center p-6 max-w-4xl mx-auto w-full">
        <div className="card w-full mb-8">
          <h2 className="section-title">자동 문제 생성</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">학교 단위</label>
              <select
                className="input-field w-full"
                value={selectedSchool}
                onChange={(e) => {
                  if (!isLoggedIn()) {
                    alert("로그인 먼저 해주세요.")
                    return
                  }
                  setSelectedSchool(e.target.value)
                }}
              >
                <option value="">선택하세요</option>
                {schools.map((school) => (
                  <option key={school} value={school}>
                    {school}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">학년</label>
              <select
                className="input-field w-full"
                value={selectedGrade}
                onChange={(e) => {
                  setSelectedGrade(e.target.value)
                }}
                disabled={!selectedSchool}
              >
                <option value="">선택하세요</option>
                {selectedSchool &&
                  grades[selectedSchool as keyof typeof grades]?.map((grade) => (
                    <option key={grade} value={grade}>
                      {grade}
                    </option>
                  ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">과목</label>
              <select
                className="input-field w-full"
                value={selectedSubject}
                onChange={(e) => {
                  setSelectedSubject(e.target.value)
                }}
                disabled={!selectedGrade}
              >
                <option value="">선택하세요</option>
                {selectedSchool &&
                  subjects[selectedSchool as keyof typeof subjects]?.map((subject) => (
                    <option key={subject} value={subject}>
                      {subject}
                    </option>
                  ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">대단원</label>
              <select
                className="input-field w-full"
                value={selectedChapter}
                onChange={(e) => setSelectedChapter(e.target.value)}
                disabled={!selectedSubject}
              >
                <option value="">선택하세요</option>
                {getChapters().map((chapter) => (
                  <option key={chapter} value={chapter}>{chapter}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">중단원</label>
              <select
                className="input-field w-full"
                value={selectedMiddle}
                onChange={(e) => setSelectedMiddle(e.target.value)}
                disabled={!selectedChapter}
              >
                <option value="">선택하세요</option>
                {getMiddleChapters().map((middle) => (
                  <option key={middle} value={middle}>{middle}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">소단원</label>
              <select
                className="input-field w-full"
                value={selectedSmall}
                onChange={(e) => setSelectedSmall(e.target.value)}
                disabled={!selectedMiddle}
              >
                <option value="">선택하세요</option>
                {getSmallChapters(selectedMiddle).map((small: string) => (
                  <option key={small} value={small}>{small}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">문제 개수</label>
              <select
                className="input-field w-full"
                value={selectedNumberOfProblems}
                onChange={(e) => {
                  setSelectedNumberOfProblems(e.target.value)
                }}
                disabled={!selectedSmall}
              >
                <option value="">선택하세요</option>
                {numberOfProblems.map((num) => (
                  <option key={num} value={num}>
                    {num}문제
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-center mt-6">
            <button
              className="btn-primary text-lg px-8 py-3 flex items-center justify-center"
              onClick={handleGenerateQuiz}
              disabled={isLoading || !selectedNumberOfProblems}
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin h-5 w-5 mr-2 text-white" viewBox="0 0 24 24">
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                      fill="none"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                    />
                  </svg>
                  문제 생성 중입니다...
                </>
              ) : (
                "문제 생성하기"
              )}
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 w-full">
          <div className="card text-center">
            <div className="mb-2">
              <Sparkles className="h-12 w-12 mx-auto text-indigo-600 dark:text-indigo-400" />
            </div>
            <h3 className="text-xl font-semibold mb-2">자동 문제 생성</h3>
            <p className="text-gray-600">GenAI를 활용해 4지선다형, OX, 주관식 문제를 자동으로 생성합니다.</p>
          </div>

          <div className="card text-center">
            <div className="mb-2">
              <QrCode className="h-12 w-12 mx-auto text-indigo-600 dark:text-indigo-400" />
            </div>
            <h3 className="text-xl font-semibold mb-2">QR 코드 공유</h3>
            <p className="text-gray-600">선생님은 생성된 퀴즈를 QR 코드로 학생들과 쉽게 공유할 수 있습니다.</p>
          </div>

          <div className="card text-center">
            <div className="mb-2">
              <BarChart3 className="h-12 w-12 mx-auto text-indigo-600 dark:text-indigo-400" />
            </div>
            <h3 className="text-xl font-semibold mb-2">성적 분석</h3>
            <p className="text-gray-600">학습 리포트를 제공해 개별 맞춤화 학습이 가능합니다.</p>
          </div>
        </div>
      </main>
    </div>
  )
}
