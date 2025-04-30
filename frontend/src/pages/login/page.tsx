import { useState, useEffect } from "react";
import { Link, useNavigate, useLocation, useSearchParams } from "react-router-dom";
import Header from "@_components/header";

export default function Login() {
  const BASE_URL = import.meta.env.VITE_BASE_URL;
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const justRegistered = searchParams.get("registered") === "true";
  const from = location.state?.from || "/";

  const [formData, setFormData] = useState({
    schoolName: "",
    schoolCode: "",
    grade: "",
    classname: "",
    studentId: "",
    password: "",
    userType: "student",
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccessMessage, setShowSuccessMessage] = useState(false);
  const [idToSend, setIdToSend] = useState("");
  const [schoolSearch, setSchoolSearch] = useState("");
  const [schoolResults, setSchoolResults] = useState<{ schoolName: string; schoolAddress: string; schoolCode: string }[]>([]);
  const [showSchoolModal, setShowSchoolModal] = useState(false);

  useEffect(() => {
    if (justRegistered) {
      setShowSuccessMessage(true);
      const timer = setTimeout(() => {
        setShowSuccessMessage(false);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [justRegistered]);

  useEffect(() => {
    let generatedId = "";
    if (formData.userType === "student" && formData.schoolCode && formData.grade && formData.classname && formData.studentId) {
      generatedId = `${formData.schoolCode}-${formData.grade}-${formData.classname}-${formData.studentId}`;
    } else if (formData.userType === "teacher" && formData.schoolCode && formData.grade && formData.classname) {
      generatedId = `${formData.schoolCode}-${formData.grade}-${formData.classname}`;
    }
    setIdToSend(generatedId);
  }, [formData]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    if (errors[name]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
  };

  const fetchSchoolCode = async () => {
    if (!formData.schoolName.trim()) return;
    try {
      const res = await fetch(`${BASE_URL}/api/school-code?schoolName=` + encodeURIComponent(formData.schoolName));
      const data = await res.json();
      setFormData((prev) => ({ ...prev, schoolCode: data.schoolCode || "" }));
    } catch (err) {
      console.error("학교 코드 조회 실패:", err);
      setFormData((prev) => ({ ...prev, schoolCode: "" }));
    }
  };

  const handleSchoolSearch = async () => {
    try {
      const res = await fetch(`${BASE_URL}/api/search-school?name=` + encodeURIComponent(schoolSearch));
      const data = await res.json();
      setSchoolResults(data.schools || []);
    } catch (err) {
      console.error("학교 검색 실패:", err);
      setSchoolResults([]);
    }
  };

  const handleSchoolSelect = (school: { schoolName: string; schoolAddress: string; schoolCode: string }) => {
    setFormData(prev => ({
      ...prev,
      schoolName: school.schoolName,
      schoolCode: school.schoolCode,
    }));
    setShowSchoolModal(false);
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.schoolName.trim()) newErrors.schoolName = "학교명을 입력해주세요.";
    if (!formData.grade.trim()) newErrors.grade = "학년을 선택해주세요.";
    if (!formData.classname.trim()) newErrors.classname = "반을 선택해주세요.";
    if (formData.userType === "student" && !formData.studentId.trim()) newErrors.studentId = "번호를 선택해주세요.";
    if (!formData.password) newErrors.password = "비밀번호를 입력해주세요.";
    if (!idToSend) newErrors.id = "ID 생성에 필요한 정보가 부족합니다.";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) return;
    setIsSubmitting(true);

    try {
      const response = await fetch(`${BASE_URL}/api/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          id: idToSend,
          password: formData.password,
        }),
      });

      if (!response.ok) {
        throw new Error("로그인 실패");
      }

      const result = await response.json();
      localStorage.setItem("user", JSON.stringify(result));
      navigate(from, { replace: true });
    } catch (error) {
      console.error("로그인 실패:", error);
      setErrors({ submit: "로그인에 실패했습니다. 정보를 다시 확인해주세요." });
    } finally {
      setIsSubmitting(false);
    }
  };

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

          {showSchoolModal && (
            <div className="fixed inset-0 bg-opacity-30 flex justify-center items-center z-50">
              <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6 w-[90%] max-w-lg">
                <h2 className="text-lg font-bold mb-4 text-gray-900 dark:text-white">학교 검색</h2>

                <div className="flex space-x-2 mb-4">
                  <input
                    type="text"
                    value={schoolSearch}
                    onChange={(e) => setSchoolSearch(e.target.value)}
                    onBlur={fetchSchoolCode}
                    placeholder="학교명을 입력하세요"
                    className="input-field flex-1"
                  />
                  <button onClick={handleSchoolSearch} className="btn-primary px-3 py-1">
                    검색
                  </button>
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
                  <button onClick={() => setShowSchoolModal(false)} className="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700">
                    닫기
                  </button>
                </div>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">

            {/* 사용자 유형 */}
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

            {/* 학교명 */}
            <div>
              <label htmlFor="schoolName" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">학교명</label>
              <input name="schoolName" value={formData.schoolName} onChange={handleChange} className="input-field w-full" />
              {errors.schoolName && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.schoolName}</p>}

              <div className="flex justify-start mt-2">
                <button type="button" onClick={() => setShowSchoolModal(true)} className="px-3 py-1 text-sm bg-indigo-100 dark:bg-indigo-800 text-indigo-700 dark:text-white rounded-md">
                  학교명 검색
                </button>
              </div>
            </div>

            {/* 학년 */}
            <div>
              <label htmlFor="grade" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">학년</label>
              <select name="grade" value={formData.grade} onChange={handleChange} className="input-field w-full">
                <option value="">학년을 선택하세요</option>
                <option value="3">3학년</option>
              </select>
              {errors.grade && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.grade}</p>}
            </div>

            {/* 반 */}
            <div>
              <label htmlFor="classname" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">반</label>
              <select name="classname" value={formData.classname} onChange={handleChange} className="input-field w-full">
                <option value="">반을 선택하세요</option>
                <option value="1">1반</option>
              </select>
              {errors.classname && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.classname}</p>}
            </div>

            {/* 번호 */}
            {formData.userType === "student" && (
              <div>
                <label htmlFor="studentId" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">번호</label>
                <select name="studentId" value={formData.studentId} onChange={handleChange} className="input-field w-full">
                  <option value="">번호를 선택하세요</option>
                  <option value="1">1번</option>
                  <option value="2">2번</option>
                </select>
                {errors.studentId && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.studentId}</p>}
              </div>
            )}

            {/* 생성된 ID 표시 */}
            {idToSend && (
              <p className="text-sm text-indigo-600 dark:text-indigo-300">ID: <strong>{idToSend}</strong></p>
            )}

            {/* 비밀번호 */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">비밀번호</label>
              <input name="password" type="password" value={formData.password} onChange={handleChange} className="input-field w-full" />
              {errors.password && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.password}</p>}
            </div>

            {/* 로그인 버튼 */}
            <div className="pt-4">
              <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
                {isSubmitting ? "로그인 중..." : "로그인"}
              </button>
            </div>

            {/* 회원가입 링크 */}
            <div className="text-center mt-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                계정이 없으신가요?{" "}
                <Link to="/register" className="text-indigo-600 hover:text-indigo-500 dark:text-indigo-400 dark:hover:text-indigo-300">
                  회원가입
                </Link>
              </p>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}
