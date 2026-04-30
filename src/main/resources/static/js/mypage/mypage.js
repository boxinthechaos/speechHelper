window.onload = function () {
    fetchMyInfo();
    fetchInterviewStats();
    fetchPortfolioSummary();
};

// ─── 프로필 정보 ───
async function fetchMyInfo() {
    try {
        const res = await customFetch('/api/v1/user/me');
        if (!res.ok) throw new Error();
        const data = await res.json();

        // 아바타 이니셜
        const initial = (data.name || '?').charAt(0).toUpperCase();
        document.getElementById('profileAvatar').textContent = initial;

        document.getElementById('profileName').textContent = data.name || '-';
        document.getElementById('profileEmail').textContent = data.email || '-';
        document.getElementById('profileJoined').textContent =
            `가입일: ${formatDate(data.createdAt)}`;

    } catch (e) {
        document.getElementById('profileName').textContent = '정보를 불러올 수 없습니다.';
        document.getElementById('profileEmail').textContent = '';
        document.getElementById('profileJoined').textContent = '';
    }
}

// ─── 면접 통계 ───
async function fetchInterviewStats() {
    try {
        const res = await customFetch('/api/v1/interview/history');
        if (!res.ok) throw new Error();
        const list = await res.json();

        document.getElementById('statInterviewTotal').textContent = `${list.length}회`;

        if (list.length > 0) {
            // 가장 최근 항목 기준 날짜
            const sorted = [...list].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
            document.getElementById('statRecentInterview').textContent =
                formatDate(sorted[0].createdAt);
        } else {
            document.getElementById('statRecentInterview').textContent = '-';
        }

    } catch (e) {
        document.getElementById('statInterviewTotal').textContent = '-';
        document.getElementById('statRecentInterview').textContent = '-';
    }
}

// ─── 포트폴리오 첨삭 요약 ───
async function fetchPortfolioSummary() {
    const container = document.getElementById('portfolioSummary');
    try {
        const res = await customFetch('/api/v1/interview/portfolio/history');
        if (!res.ok) throw new Error();
        const list = await res.json();

        document.getElementById('statPortfolioTotal').textContent = `${list.length}회`;

        container.innerHTML = '';

        if (list.length === 0) {
            container.innerHTML = '<div class="empty-summary">아직 포트폴리오 첨삭 기록이 없습니다. 📂</div>';
            return;
        }

        // 최신순 최대 5개
        const recent = [...list]
            .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
            .slice(0, 5);

        recent.forEach(item => {
            const row = document.createElement('div');
            row.className = 'portfolio-row';
            row.innerHTML = `
                <span class="portfolio-row-url">🔗 ${item.portfolioUrl}</span>
                <span class="portfolio-row-date">${formatDate(item.createdAt)}</span>
            `;
            container.appendChild(row);
        });

    } catch (e) {
        document.getElementById('statPortfolioTotal').textContent = '-';
        container.innerHTML = '<div class="empty-summary" style="color:#ef4444;">데이터를 불러오는 데 실패했습니다.</div>';
    }
}

// ─── 날짜 포맷 ───
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`;
}