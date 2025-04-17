import { useState, useEffect } from "react"
import { Link, useNavigate, useLocation, useSearchParams } from "react-router-dom"
import Header from "@_components/header"

export default function Login() {
  const BASE_URL = import.meta.env.VITE_BASE_URL
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const justRegistered = searchParams.get("registered") === "true"
  const from = location.state?.from || "/"

  const [formData, setFormData] = useState({
    Id: "",
    password: "",
    rememberMe: false,
  })

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showSuccessMessage, setShowSuccessMessage] = useState(false)

  useEffect(() => {
    if (justRegistered) {
      setShowSuccessMessage(true)
      const timer = setTimeout(() => {
        setShowSuccessMessage(false)
      }, 5000)
      return () => clearTimeout(timer)
    }
  }, [justRegistered])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }))

    if (errors[name]) {
      setErrors((prev) => {
        const newErrors = { ...prev }
        delete newErrors[name]
        return newErrors
      })
    }
  }

  const validateForm = () => {
    const newErrors: Record<string, string> = {}
    if (!formData.Id.trim()) newErrors.Id = "아이디를 입력해주세요."
    if (!formData.password) newErrors.password = "비밀번호를 입력해주세요."
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!validateForm()) return
    setIsSubmitting(true)

    try {
      const response = await fetch(`${BASE_URL}/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          id: formData.Id,
          password: formData.password,
        }),
      })

      if (!response.ok) {
        throw new Error("로그인 실패")
      }

      const result = await response.json()
      localStorage.setItem("user", JSON.stringify(result))

      // ✅ 로그인 성공 후 원래 있던 경로로 이동, 없으면 홈
      navigate(from, { replace: true })
    } catch (error) {
      console.error("로그인 실패:", error)
      setErrors({ submit: "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요." })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="max-w-md mx-auto px-4 py-8">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6 text-center">로그인</h1>

          {showSuccessMessage && (
            <div className="mb-4 p-3 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded-md">
              회원가입이 완료되었습니다. 로그인해주세요.
            </div>
          )}

          {errors.submit && (
            <div className="mb-4 p-3 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded-md">
              {errors.submit}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="Id" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                아이디
              </label>
              <input
                type="text"
                id="Id"
                name="Id"
                value={formData.Id}
                onChange={handleChange}
                className={`input-field w-full ${errors.Id ? "border-red-500 dark:border-red-500" : ""}`}
                placeholder="아이디를 입력하세요"
              />
              {errors.Id && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.Id}</p>}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                비밀번호
              </label>
              <input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className={`input-field w-full ${errors.password ? "border-red-500 dark:border-red-500" : ""}`}
                placeholder="비밀번호를 입력하세요"
              />
              {errors.password && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.password}</p>}
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="rememberMe"
                  name="rememberMe"
                  type="checkbox"
                  checked={formData.rememberMe}
                  onChange={handleChange}
                  className="h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                />
                <label htmlFor="rememberMe" className="ml-2 block text-sm text-gray-700 dark:text-gray-300">
                  로그인 상태 유지
                </label>
              </div>
            </div>

            <div className="pt-4">
              <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
                {isSubmitting ? (
                  <span className="flex items-center justify-center">
                    <svg
                      className="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      />
                    </svg>
                    로그인 중...
                  </span>
                ) : (
                  "로그인"
                )}
              </button>
            </div>

            <div className="text-center mt-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                계정이 없으신가요?{" "}
                <Link
                  to="/register"
                  className="text-indigo-600 hover:text-indigo-500 dark:text-indigo-400 dark:hover:text-indigo-300"
                >
                  회원가입
                </Link>
              </p>
            </div>
          </form>
        </div>
      </main>
    </div>
  )
}
