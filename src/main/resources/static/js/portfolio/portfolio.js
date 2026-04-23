const API_BASE = '/api/v1/interview';

window.addEventListener('DOMContentLoaded', loadHistory);

async function loadHistory() {
    try {
        const res = await fetch(`${API_BASE}/portfolio/history`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        renderHistory(data);
    } catch (e) {
    }
}

function renderHistory(list) {
    const container = document.getElementById('historyList');
    if (!list || list.length === 0) {
        container.innerHTML = `<div class="history-empty">아직 평가 기록이 없습니다.<br>위에서 포트폴리오 URL을 입력해 첫 평가를 받아보세요!</div>`;
        return;
    }

    container.innerHTML = '';

    list.forEach(item => {
        const card = document.createElement('div');
        card.className = 'history-card';

        card.innerHTML = `
            <div class="history-card-header">
                <span class="history-url">🔗 ${item.portfolioUrl}</span>
                <span class="history-date">${formatDate(item.createdAt)}</span>
            </div>
            <div class="history-btn-row">
                <button class="btn-view">전체 보기</button>
                <button class="btn-delete">삭제</button>
            </div>
        `;

        card.querySelector('.btn-view').addEventListener('click', () => {
            viewResult(item.portfolioUrl, item.feedback);
        });
        card.querySelector('.btn-delete').addEventListener('click', () => {
            deleteHistory(item.id);
        });

        container.appendChild(card);
    });
}

async function evaluatePortfolio() {
    const url = document.getElementById('portfolioUrl').value.trim();
    if (!url) { showToast('URL을 입력해주세요!', true); return; }
    if (!url.startsWith('http')) { showToast('올바른 URL을 입력해주세요!', true); return; }

    openModal();

    try {
        const res = await fetch(`${API_BASE}/portfolio/evaluate?portfolioUrl=${encodeURIComponent(url)}`, {
            method: 'POST'
        });
        if (!res.ok) throw new Error();
        const feedback = await res.text();
        showResult(url, feedback);
        loadHistory();
    } catch (e) {
        closeModal();
        showToast('평가 중 오류가 발생했습니다. 다시 시도해주세요.', true);
    }
}

async function deleteHistory(id) {
    if (!confirm('이 평가 기록을 삭제할까요?')) return;
    try {
        const res = await fetch(`${API_BASE}/portfolio/history/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error();
        showToast('삭제되었습니다!');
        loadHistory();
    } catch (e) {
        showToast('삭제 중 오류가 발생했습니다.', true);
    }
}

function viewResult(url, feedback) {
    openModal();
    showResult(url, feedback);
}

function openModal() {
    document.getElementById('modalLoading').style.display = 'flex';
    document.getElementById('modalResult').style.display = 'none';
    document.getElementById('modalOverlay').classList.add('active');
}

function showResult(url, feedback) {
    document.getElementById('modalLoading').style.display = 'none';
    document.getElementById('resultUrl').textContent = url;
    document.getElementById('feedbackText').textContent = feedback;
    document.getElementById('modalResult').style.display = 'block';
}

function closeModal() {
    document.getElementById('modalOverlay').classList.remove('active');
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`;
}

function escapeAttr(str) {
    return (str || '').replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

function escapeTemplate(str) {
    return (str || '').replace(/`/g, '\\`').replace(/\$/g, '\\$');
}

// Enter 키로 제출
document.getElementById('portfolioUrl').addEventListener('keydown', e => {
    if (e.key === 'Enter') evaluatePortfolio();
});

// 모달 외부 클릭 시 닫기
document.getElementById('modalOverlay').addEventListener('click', function(e) {
    if (e.target === this) closeModal();
});