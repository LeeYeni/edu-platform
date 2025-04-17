import { Link, useNavigate, useLocation } from "react-router-dom"
import { useEffect, useState } from "react"

interface UserInfo {
  userId: string;
  userType: string;
  name: string;
  schoolCode: string;
  grade: string;
  classname: string;
}

export default function Header() {
  const navigate = useNavigate()
  const location = useLocation()

  const [userInfo, setUserInfo] = useState<UserInfo | null>(null)

  useEffect(() => {
    const storedUser = localStorage.getItem("user")
    if (storedUser) {
      try {
        setUserInfo(JSON.parse(storedUser))
      } catch {
        setUserInfo(null)
      }
    }
  }, [])

  const handleLogout = () => {
    localStorage.removeItem("user")
    setUserInfo(null)
    navigate("/")
  }

  return (
    <header className="bg-white shadow-sm dark:bg-gray-800 dark:border-b dark:border-gray-700 transition-colors duration-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center">
        <Link to="/" className="text-2xl font-bold text-indigo-600 dark:text-indigo-400">
          퀴즈 생성기
        </Link>

        <div className="flex items-center space-x-4">
          {userInfo ? (
            <>
              <Link
                to={
                  userInfo.userType === "teacher"
                    ? `/quiz/room/${userInfo.userId}`
                    : `/quiz/room/${userInfo.userId
                        .split("-")
                        .slice(0, 3)
                        .join("-")}`
                }
                className={`${
                  location.pathname.startsWith("/quiz/room")
                    ? "font-bold text-indigo-600"
                    : "text-gray-700"
                } hover:text-indigo-600`}
              >
                우리반
              </Link>

              {userInfo.userType === "student" && (
                <Link
                  to="/mypage"
                  className={`${
                    location.pathname === "/mypage"
                      ? "font-bold text-indigo-600"
                      : "text-gray-700"
                  } hover:text-indigo-600`}
                >
                  마이페이지
                </Link>
              )}

              <span className="text-gray-700 dark:text-gray-300">
                <strong>{userInfo.name}</strong>{" "}
                {userInfo.userType === "student"
                  ? "(학생)"
                  : userInfo.userType === "teacher"
                  ? "(선생님)"
                  : "(관리자)"}
              </span>

              <button
                onClick={handleLogout}
                className="text-gray-700 hover:text-indigo-600 dark:text-gray-300 dark:hover:text-indigo-400"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-gray-700 hover:text-indigo-600 dark:text-gray-300 dark:hover:text-indigo-400"
              >
                로그인
              </Link>
              <Link to="/register" className="btn-primary">
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}

