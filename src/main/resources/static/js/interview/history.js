window.onload = function () {
    fetchMyHistory();
    fetchPortfolioHistory();
};

// ─── 탭 전환 ───
function switchTab(tab) {
    const isInterview = tab === 'interview';

    document.getElementById('panelInterview').style.display = isInterview ? 'block' : 'none';
    document.getElementById('panelPortfolio').style.display = isInterview ? 'none' : 'block';

    document.getElementById('tabInterview').classList.toggle('active', isInterview);
    document.getElementById('tabPortfolio').classList.toggle('active', !isInterview);
}

// ─── 면접 기록 ───
async function fetchMyHistory() {
    const container = document.getElementById('historyContainer');
    const countBadge = document.getElementById('historyCount');
    if (!container || !countBadge) return;

    try {
        const response = await customFetch('/api/v1/interview/history');
        if (!response.ok) throw new Error('서버 에러');

        const historyList = await response.json();
        container.innerHTML = '';
        countBadge.textContent = `총 ${historyList.length}개`;

        if (historyList.length === 0) {
            container.innerHTML = '<div class="empty-history">아직 기록이 없습니다. 면접을 먼저 진행해보세요! 🏃‍♂️</div>';
            return;
        }

        [...historyList].reverse().forEach(item => {
            const preview = item.answer.length > 40 ? item.answer.slice(0, 40) + '…' : item.answer;
            const div = document.createElement('div');
            div.className = 'history-item';
            div.innerHTML = `
                <div class="history-item-header" onclick="toggleHistory(this.closest('.history-item'))">
                    <div class="history-item-meta">
                        <div class="history-item-question">Q. ${item.question}</div>
                        <div class="history-item-preview">🗣️ ${preview}</div>
                    </div>
                    <button class="btn-delete" onclick="event.stopPropagation(); deleteHistoryItem(${item.id})">삭제</button>
                    <svg class="history-chevron" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7"/>
                    </svg>
                </div>
                <div class="history-item-body">
                    <div class="history-item-body-inner">
                        <div class="my-answer"><strong>🗣️ 내 답변</strong><br>${item.answer}</div>
                        <div class="ai-feedback"><strong>🤖 면접관 피드백</strong><br>${item.feedback}</div>
                    </div>
                </div>
            `;
            container.appendChild(div);
        });
    } catch (error) {
        console.error("면접 기록 불러오기 실패", error);
        container.innerHTML = '<div class="empty-history" style="color:#ef4444;">데이터를 불러오는 데 실패했습니다.</div>';
    }
}

// ─── 포트폴리오 첨삭 기록 ───
async function fetchPortfolioHistory() {
    const container = document.getElementById('portfolioContainer');
    const countBadge = document.getElementById('portfolioCount');
    if (!container || !countBadge) return;

    try {
        const response = await customFetch('/api/v1/interview/portfolio/history');
        if (!response.ok) throw new Error('서버 에러');

        const list = await response.json();
        container.innerHTML = '';
        countBadge.textContent = `총 ${list.length}개`;

        if (list.length === 0) {
            container.innerHTML = '<div class="empty-history">아직 포트폴리오 첨삭 기록이 없습니다. 평가를 받아보세요! 📂</div>';
            return;
        }

        [...list].reverse().forEach(item => {
            const div = document.createElement('div');
            div.className = 'history-item';
            div.innerHTML = `
                <div class="history-item-header" onclick="toggleHistory(this.closest('.history-item'))">
                    <div class="history-item-meta">
                        <div class="history-item-question">🔗 ${item.portfolioUrl}</div>
                        <div class="history-item-preview">📅 ${formatDate(item.createdAt)}</div>
                    </div>
                    <button class="btn-delete" onclick="event.stopPropagation(); deletePortfolioItem(${item.id})">삭제</button>
                    <svg class="history-chevron" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7"/>
                    </svg>
                </div>
                <div class="history-item-body">
                    <div class="history-item-body-inner">
                        <div class="ai-feedback"><strong>🤖 AI 첨삭 결과</strong><br>${item.feedback}</div>
                    </div>
                </div>
            `;
            container.appendChild(div);
        });
    } catch (error) {
        console.error("포트폴리오 기록 불러오기 실패", error);
        container.innerHTML = '<div class="empty-history" style="color:#ef4444;">데이터를 불러오는 데 실패했습니다.</div>';
    }
}

// ─── 날짜 포맷 ───
function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`;
}

// ─── 아코디언 ───
function toggleHistory(item) {
    item.classList.toggle('open');
}

// ─── 면접 기록 삭제 ───
async function deleteHistoryItem(id) {
    if (!confirm('이 팩폭 기록을 영구히 삭제하시겠습니까? 🗑️')) return;
    try {
        const response = await customFetch(`/api/v1/interview/history/${id}`, { method: 'DELETE' });
        if (response.ok) {
            showToast('기록이 삭제되었습니다.');
            fetchMyHistory();
        } else {
            showToast('삭제 실패 (상태 코드: ' + response.status + ')', true);
        }
    } catch (error) {
        console.error(error);
        showToast('서버 통신 오류가 발생했습니다.', true);
    }
}

// ─── 포트폴리오 기록 삭제 ───
async function deletePortfolioItem(id) {
    if (!confirm('이 첨삭 기록을 삭제하시겠습니까? 🗑️')) return;
    try {
        const response = await customFetch(`/api/v1/interview/portfolio/history/${id}`, { method: 'DELETE' });
        if (response.ok) {
            showToast('기록이 삭제되었습니다.');
            fetchPortfolioHistory();
        } else {
            showToast('삭제 실패 (상태 코드: ' + response.status + ')', true);
        }
    } catch (error) {
        console.error(error);
        showToast('서버 통신 오류가 발생했습니다.', true);
    }
}