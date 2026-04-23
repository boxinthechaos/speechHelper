let allQuestionsData = []; // 서버에서 받아온 전체 질문 저장용
let currentFeedback = "";
let selectedQuestionId = null;

const loadingMessages = [
    "면접관들이 답변을 검토 중입니다...",
    "논리적 허점을 찾는 중입니다... 🔍",
    "팩폭을 준비하고 있습니다... 🥊",
    "정답과 대조하는 중입니다...",
    "피드백을 작성하는 중입니다... ✍️",
];
let loadingMsgInterval = null;

window.onload = function () {
    fetchQuestions();
};

// ─── 모달 열기/닫기 로직 ───
function openModalLoading() {
    const overlay = document.getElementById('feedbackModal');
    document.getElementById('modalLoading').style.display = 'flex';
    document.getElementById('modalResult').style.display = 'none';
    overlay.classList.add('active');

    let msgIndex = 0;
    const textEl = document.getElementById('loadingText');
    textEl.textContent = loadingMessages[0];
    loadingMsgInterval = setInterval(() => {
        msgIndex = (msgIndex + 1) % loadingMessages.length;
        textEl.style.opacity = '0';
        setTimeout(() => {
            textEl.textContent = loadingMessages[msgIndex];
            textEl.style.opacity = '1';
        }, 300);
    }, 2200);
}

function showModalResult(feedbackText) {
    clearInterval(loadingMsgInterval);
    document.getElementById('modalLoading').style.display = 'none';
    const resultEl = document.getElementById('modalResult');
    resultEl.style.display = 'block';
    document.getElementById('modalFeedbackText').innerText = feedbackText;
}

function closeModal() {
    clearInterval(loadingMsgInterval);
    stopTTS();
    document.getElementById('feedbackModal').classList.remove('active');
}

function handleOverlayClick(e) {
    if (e.target === document.getElementById('feedbackModal')) closeModal();
}
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });


// ─── 1. 전체 질문 데이터 가져오기 및 카테고리 렌더링 ───
async function fetchQuestions() {
    try {
        const response = await customFetch('/api/v1/interview/questions');
        if (response.ok) {
            allQuestionsData = await response.json();
            renderCategories();
        }
    } catch (error) {
        console.error("질문 목록 실패", error);
        document.getElementById('categoryCards').innerHTML =
            '<div style="color:#ef4444; padding:20px;">서버에서 데이터를 가져오지 못했습니다.</div>';
    }
}

// ─── 2. 카테고리 카드 그리기 ───
function renderCategories() {
    // 중복 제거하여 카테고리 목록만 추출
    const categories = [...new Set(allQuestionsData.map(q => q.category))];
    const categoryCardsGrid = document.getElementById('categoryCards');
    categoryCardsGrid.innerHTML = '';

    if (categories.length === 0) {
        categoryCardsGrid.innerHTML = '<div style="color:#cbd5e1;">등록된 질문이 없습니다.</div>';
        return;
    }

    categories.forEach(category => {
        // 해당 카테고리에 몇 개의 질문이 있는지 계산
        const count = allQuestionsData.filter(q => q.category === category).length;

        const card = document.createElement('div');
        card.className = 'question-card category-card'; // 카테고리용 스타일 클래스 추가
        card.innerHTML = `<div style="font-size: 1.2em; font-weight: 800; color: #fff;">📁 ${category}</div>
                          <div style="font-size: 0.8em; color: #94a3b8; margin-top: 5px;">${count}개의 질문</div>`;
        card.onclick = () => showQuestionsByCategory(category);
        categoryCardsGrid.appendChild(card);
    });
}

// ─── 3. 특정 카테고리 클릭 시 질문 리스트 보여주기 ───
function showQuestionsByCategory(category) {
    document.getElementById('categoryView').style.display = 'none';
    document.getElementById('questionView').style.display = 'block';
    document.getElementById('currentCategoryTitle').innerText = `[${category}] 질문 리스트`;

    const questionGrid = document.getElementById('questionCards');
    questionGrid.innerHTML = '';

    const filteredQuestions = allQuestionsData.filter(q => q.category === category);

    filteredQuestions.forEach(q => {
        const card = document.createElement('div');
        card.className = 'question-card';
        card.innerHTML = `Q. ${q.question}`;
        card.onclick = () => selectCard(card, q.id, q.question);
        questionGrid.appendChild(card);
    });
}

// ─── 4. 뒤로가기 (카테고리 뷰로 돌아가기) ───
function goBackToCategories() {
    document.getElementById('questionView').style.display = 'none';
    document.getElementById('answerSection').style.display = 'none';
    document.getElementById('categoryView').style.display = 'block';
    selectedQuestionId = null;
}

// ─── 5. 특정 질문 카드 선택 시 답변 창 열기 ───
function selectCard(clickedCard, id, questionText) {
    document.querySelectorAll('#questionCards .question-card').forEach(c => c.classList.remove('active'));
    clickedCard.classList.add('active');
    selectedQuestionId = id;

    const answerSection = document.getElementById('answerSection');
    answerSection.style.display = 'block';
    document.getElementById('selectedQuestionTitle').innerText = `Q. ${questionText}`;
    document.getElementById('answer').value = '';
    document.getElementById('answer').focus();
    answerSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// ─── 6. 답변 제출 & 피드백 ───
async function getFeedback() {
    const answer = document.getElementById('answer').value.trim();

    if (!selectedQuestionId) { showToast('질문 카드를 먼저 선택해주세요!', true); return; }
    if (!answer)              { showToast('답변을 입력해주세요!', true); return; }

    openModalLoading();

    try {
        const response = await customFetch('/api/v1/interview/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ questionId: selectedQuestionId, answer })
        });

        if (response.ok) {
            const resultData = await response.json();
            currentFeedback = resultData.feedback;
            showModalResult(currentFeedback);
            // 기록 갱신 함수 삭제함 (fetchMyHistory 제거)
        } else {
            showModalResult('❌ 서버 에러가 발생했습니다.');
        }
    } catch (error) {
        closeModal();
        showToast('통신 실패! 백엔드 서버를 확인하세요.', true);
    }
}

// ─── 7. TTS ───
function playTTS() {
    if (!currentFeedback) return;
    const cleanText = currentFeedback.replace(/[*#\-\_]/g, '').replace(/\[|\]/g, '').replace(/\n+/g, ' ').trim();
    window.speechSynthesis.cancel();
    const speech = new SpeechSynthesisUtterance(cleanText);
    speech.lang = 'ko-KR'; speech.rate = 1.1; speech.pitch = 1.0;
    window.speechSynthesis.speak(speech);
}
function stopTTS() { window.speechSynthesis.cancel(); }

let recognition = null;
let isListening = false;

function toggleSTT() {
    if (isListening) {
        stopSTT();
    } else {
        startSTT();
    }
}

function startSTT() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        showToast('이 브라우저는 음성 인식을 지원하지 않습니다.', true);
        return;
    }

    recognition = new SpeechRecognition();
    recognition.lang = 'ko-KR';
    recognition.interimResults = true;   // 중간 결과도 표시
    recognition.continuous = true;       // 말 멈춰도 계속 인식

    const textarea = document.getElementById('answer');
    const micBtn = document.getElementById('micBtn');
    const micStatus = document.getElementById('micStatus');
    let finalTranscript = textarea.value; // 기존 텍스트 유지

    recognition.onstart = () => {
        isListening = true;
        micBtn.textContent = '⏹️ 멈추기';
        micBtn.style.background = 'linear-gradient(135deg, #991b1b, #b91c1c)';
        micStatus.textContent = '🔴 듣는 중...';
    };

    recognition.onresult = (e) => {
        let interim = '';
        for (let i = e.resultIndex; i < e.results.length; i++) {
            if (e.results[i].isFinal) {
                finalTranscript += e.results[i][0].transcript;
            } else {
                interim = e.results[i][0].transcript;
            }
        }
        textarea.value = finalTranscript + interim;
    };

    recognition.onend = () => {
        // continuous=true인데 끊기면 재시작 (조용한 경우 방지)
        if (isListening) recognition.start();
    };

    recognition.onerror = (e) => {
        if (e.error !== 'no-speech') {
            showToast(`음성 인식 오류: ${e.error}`, true);
            stopSTT();
        }
    };

    recognition.start();
}

function stopSTT() {
    if (recognition) {
        isListening = false;
        recognition.onend = null; // 재시작 방지
        recognition.stop();
        recognition = null;
    }
    const micBtn = document.getElementById('micBtn');
    micBtn.textContent = '🎤 말하기';
    micBtn.style.background = '';
    document.getElementById('micStatus').textContent = '';
}