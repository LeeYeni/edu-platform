import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import Header from "@_components/header"

export default function Register() {
  const navigate = useNavigate()
  const BASE_URL = import.meta.env.VITE_BASE_URL;

  const [formData, setFormData] = useState({
      Id: "",
    name: "",
    password: "",
    confirmPassword: "",
    userType: "student",
    schoolName: "",
    schoolCode: "",
    grade: "",
    classname: "",
    studentId: "",
  })

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const [showSchoolModal, setShowSchoolModal] = useState(false)
  const [schoolSearch, setSchoolSearch] = useState("")
  const [schoolResults, setSchoolResults] = useState<{ schoolName: string; schoolAddress: string; schoolCode: string }[]>([])

  const [idExists, setIdExists] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target

    setFormData((prev) => {
      const updated = { ...prev, [name]: value }

      if (
        updated.userType === "student" &&
        updated.schoolCode &&
        updated.grade &&
        updated.classname &&
        updated.studentId
      ) {
        updated.Id = `${updated.schoolCode}-${updated.grade}-${updated.classname}-${updated.studentId}`
      } else if (
          updated.userType === "teacher" &&
          updated.schoolCode &&
          updated.grade &&
          updated.classname
      ) {
        updated.Id = `${updated.schoolCode}-${updated.grade}-${updated.classname}`
      } else {
        updated.Id = "" // 일부 값이 빠져 있으면 초기화
      }

      return updated
    })

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

    if (!formData.name.trim()) newErrors.name = "이름을 입력해주세요."
    if (!formData.schoolName.trim()) newErrors.schoolName = "학교 이름을 입력해주세요."
    if (!formData.schoolCode.trim()) newErrors.schoolCode = "유효한 학교명을 입력해주세요."
    if (!formData.grade) newErrors.grade = "학년을 선택해주세요."
    if (!formData.classname) newErrors.classname = "반을 선택해주세요."

    if (formData.userType === "student") {
      if (!formData.studentId.trim()) newErrors.studentId = "번호를 입력해주세요."
    }

    if (!formData.password) {
      newErrors.password = "비밀번호를 입력해주세요."
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = "비밀번호가 일치하지 않습니다."
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const fetchSchoolCode = async () => {
    if (!formData.schoolName.trim()) return
    try {
      const res = await fetch(`${BASE_URL}/api/school-code?schoolName=` + encodeURIComponent(formData.schoolName))
      const data = await res.json()
      setFormData(prev => ({ ...prev, schoolCode: data.schoolCode || "" }))
    } catch (err) {
      console.error("학교 코드 조회 실패:", err)
      setFormData(prev => ({ ...prev, schoolCode: "" }))
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validateForm()) return

    setIsSubmitting(true)

    try {
      const response = await fetch(`${BASE_URL}/api/signup`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      })

      if (!response.ok) throw new Error("회원가입 실패")

      alert("회원가입이 완료되었습니다!")
      navigate("/login?registered=true")
    } catch (err) {
      console.error(err)
      setErrors({ submit: "회원가입 중 오류가 발생했습니다." })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSchoolSearch = async () => {
    try {
      const res = await fetch(`${BASE_URL}/api/search-school?name=` + encodeURIComponent(schoolSearch))
      const data = await res.json()
      setSchoolResults(data.schools || [])
    } catch (err) {
      console.error("학교 검색 실패:", err)
      setSchoolResults([])
    }
  }

  const handleSchoolSelect = (school: { schoolName: string; schoolAddress: string; schoolCode: string }) => {
    setFormData(prev => ({
      ...prev,
      schoolName: school.schoolName,
      schoolCode: school.schoolCode,
    }))
    setShowSchoolModal(false)
  }

  // 1️⃣ ID를 자동 생성해주는 useEffect
  useEffect(() => {
    const { userType, schoolCode, grade, classname, studentId } = formData;

    let generatedId = "";

    if (userType === "student" && schoolCode && grade && classname && studentId) {
      generatedId = `${schoolCode}-${grade}-${classname}-${studentId}`;
    } else if (userType === "teacher" && schoolCode && grade && classname) {
      generatedId = `${schoolCode}-${grade}-${classname}`;
    }

    setFormData(prev => ({
      ...prev,
      Id: generatedId
    }));
  }, [
    formData.userType,
    formData.schoolCode,
    formData.grade,
    formData.classname,
    formData.studentId
  ]);

  // 2️⃣ ID 중복 체크 useEffect (기존 코드 유지)
  useEffect(() => {
    const checkDuplicateId = async () => {
      if (!formData.Id) {
        setIdExists(false);
        return;
      }

      try {
        const res = await fetch(`${BASE_URL}/api/check-id?id=${formData.Id}`);
        const data = await res.json();
        setIdExists(data.exists); // { exists: true } 형태 응답
      } catch (err) {
        console.error("ID 중복 확인 실패:", err);
        setIdExists(false);
      }
    };

    checkDuplicateId();
  }, [formData.Id]);



  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="max-w-md mx-auto px-4 py-8">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6 text-center">회원가입</h1>

          {errors.submit && (
            <div className="mb-4 p-3 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded-md">
              {errors.submit}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/*사용자 유형*/}
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">사용자 유형</label>
              <div className="flex space-x-4">
                {["student", "teacher"].map(type => (
                <label key={type} className="inline-flex items-center">
                <input
                  type="radio"
                  name="userType"
                  value={type}
                  checked={formData.userType === type}
                  onChange={handleChange}
                  className="form-radio h-4 w-4 text-indigo-600"
                />
                <span className="ml-2 text-gray-700 dark:text-gray-300">{type === "student" ? "학생" : "선생님"}</span>
                </label>
                ))}
              </div>
            </div>

            {/*학교 이름*/}
            <div>
              <label htmlFor="schoolName" className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">학교 이름</label>
              <input
                type="text"
                id="schoolName"
                name="schoolName"
                value={formData.schoolName}
                onChange={handleChange}
                onBlur={fetchSchoolCode}
                className={`input-field w-full ${errors.schoolName ? "border-red-500 dark:border-red-500" : ""}`}
                placeholder="학교 이름을 입력하세요"
              />
              {errors.schoolName && <p className="text-sm text-red-600 dark:text-red-400 mt-1">{errors.schoolName}</p>}
              <div className="flex items-center space-x-2 mt-2">
                <button
                  type="button"
                  onClick={() => setShowSchoolModal(true)}
                  className="px-3 py-1 text-sm bg-indigo-100 dark:bg-indigo-800 text-indigo-700 dark:text-white rounded-md"
                >
                  학교명 검색
                </button>
              </div>
            </div>

            {/* 학년 */}
                          <div>
                          <label htmlFor="grade" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">학년</label>
                                          <select
                                            id="grade"
                                            name="grade"
                                            value={formData.grade}
                                            onChange={handleChange}
                                            className={`input-field w-full ${errors.grade ? "border-red-500 dark:border-red-500" : ""}`}
                                          >
                                            <option value="">학년을 선택하세요</option>
                                            <option value="3">3학년</option>
                                          </select>
                                          {errors.grade && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.grade}</p>}
                                        </div>

                                        {/* 반 */}
                                                        <div>
                                                          <label htmlFor="classname" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                                            반
                                                          </label>
                                                          <select
                                                            id="classname"
                                                            name="classname"
                                                            value={formData.classname}
                                                            onChange={handleChange}
                                                            className={`input-field w-full ${errors.classname ? "border-red-500 dark:border-red-500" : ""}`}
                                                          >
                                                            <option value="">반을 선택하세요</option>
                                                            <option value="1">1반</option>
                                                          </select>
                                                          {errors.classname && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.classname}</p>}
                                                        </div>

            {/* 학생인 경우 추가 필드 */}
            {formData.userType === "student" && (
              <>
                            {/* 번호 */}
                            <div>
                                                                          <label htmlFor="studentId" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                                                            번호
                                                                          </label>
                                                                          <select
                                                                            id="studentId"
                                                                            name="studentId"
                                                                            value={formData.studentId}
                                                                            onChange={handleChange}
                                                                            className={`input-field w-full ${errors.studentId ? "border-red-500 dark:border-red-500" : ""}`}
                                                                          >
                                                                            <option value="">번호를 선택하세요</option>
                                                                            <option value="1">1번</option>
                                                                            <option value="2">2번</option>
                                                                          </select>
                                                                          {errors.studentId && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.studentId}</p>}
                                                                        </div>

                          </>
                        )}

             {formData.Id && !idExists && (
               <p className="text-sm text-indigo-600 dark:text-indigo-300">
                 id는 <strong>{formData.Id}</strong>입니다.
               </p>
             )}

             {formData.Id && idExists && (
               <p className="text-sm text-red-600 dark:text-red-400">
                 이미 존재하는 회원입니다.
               </p>
             )}


            {/*이름*/}
            <div>
              <label htmlFor="name" className="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-300">이름</label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                className={`input-field w-full ${errors.name ? "border-red-500 dark:border-red-500" : ""}`}
                placeholder="이름을 입력하세요"
              />
              {errors.name && <p className="text-sm text-red-600 dark:text-red-400 mt-1">{errors.name}</p>}
            </div>

            {/* 비밀번호 */}
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

                                {/* 비밀번호 확인 */}
                                <div>
                                  <label
                                    htmlFor="confirmPassword"
                                    className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
                                  >
                                    비밀번호 확인
                                  </label>
                                  <input
                                    type="password"
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    className={`input-field w-full ${errors.confirmPassword ? "border-red-500 dark:border-red-500" : ""}`}
                                    placeholder="비밀번호를 다시 입력하세요"
                                  />
                                  {errors.confirmPassword && (
                                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.confirmPassword}</p>
                                  )}
                                </div>

            {/*제출 버튼*/}
            {!idExists && (
            <div className="pt-4">
              <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
                {isSubmitting ? "처리 중..." : "회원가입"}
              </button>
            </div>
            )}
          </form>
        </div>
      </main>

      {/* 학교 검색 모달 */}
      {showSchoolModal && (
        <div className="fixed inset-0 bg-opacity-50 flex justify-center items-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6 w-[90%] max-w-lg">
            <h2 className="text-lg font-bold mb-4 text-gray-900 dark:text-white">학교 검색</h2>
            <div className="flex space-x-2 mb-4">
              <input
                type="text"
                value={schoolSearch}
                onChange={(e) => setSchoolSearch(e.target.value)}
                placeholder="학교명을 입력하세요"
                className="input-field flex-1"
              />
              <button onClick={handleSchoolSearch} className="btn-primary px-3">검색</button>
            </div>

            {schoolResults.length > 0 ? (
              <div className="max-h-64 overflow-y-auto border rounded">
                <table className="w-full text-sm text-left">
                  <thead className="text-gray-600 dark:text-gray-300 border-b">
                    <tr>
                      <th className="py-1 px-2">학교명</th>
                      <th className="py-1 px-2">주소</th>
                    </tr>
                  </thead>
                  <tbody>
                    {schoolResults.map((school, idx) => (
                      <tr
                        key={idx}
                        className="cursor-pointer hover:bg-indigo-50 dark:hover:bg-indigo-900"
                        onClick={() => handleSchoolSelect(school)}
                      >
                        <td className="py-1 px-2">{school.schoolName}</td>
                        <td className="py-1 px-2">{school.schoolAddress}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-gray-500 dark:text-gray-400 text-sm">검색 결과가 없습니다.</p>
            )}

            <div className="flex justify-end mt-4">
              <button onClick={() => setShowSchoolModal(false)} className="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700">닫기</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
