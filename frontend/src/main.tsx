import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from './home'
import QuizShare from "./pages/quiz/QuizShare";
import QuizRoom from "./pages/quiz/room/QuizRoom";
import Register from "./pages/register/page";
import Login from "./pages/login/page";
import MyPage from "./pages/mypage/page";
import QuizPlay from "./pages/quiz/play/page";
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/quiz/share/:roomCode" element={<QuizShare />} />
        <Route path="/quiz/room/:roomCode" element={<QuizRoom />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/quiz/play/:roomCode" element={<QuizPlay />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)