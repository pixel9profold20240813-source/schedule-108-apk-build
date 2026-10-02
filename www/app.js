// ---------- 工具 ----------
function timeToMinutes(t) {
  const [h, m] = t.split(":").map(Number);
  return h * 60 + m;
}
function minutesToClock(mins) {
  const h = Math.floor(mins / 60), m = mins % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}
function formatCountdown(totalSec) {
  const m = Math.floor(totalSec / 60);
  const s = totalSec % 60;
  return `${m}:${String(s).padStart(2, "0")}`;
}

// 攤平成一週依時間排序的課程清單（跳過沒課的空堂）
const WEEK_ENTRIES = [];
for (let wd = 1; wd <= 5; wd++) {
  for (const period of PERIODS) {
    const entry = SCHEDULE[period.id][wd];
    if (entry) WEEK_ENTRIES.push({ wd, period, ...entry });
  }
}

function getStatus(now) {
  const dow = now.getDay(); // 0 Sun .. 6 Sat
  const nowMin = now.getHours() * 60 + now.getMinutes();

  const inSchoolDay = dow >= 1 && dow <= 5;
  let current = null;

  if (inSchoolDay) {
    current = WEEK_ENTRIES.find(
      (e) =>
        e.wd === dow &&
        nowMin >= timeToMinutes(e.period.start) &&
        nowMin < timeToMinutes(e.period.end)
    ) || null;
  }

  let next =
    WEEK_ENTRIES.find(
      (e) => e.wd > dow || (e.wd === dow && timeToMinutes(e.period.start) > nowMin)
    ) || WEEK_ENTRIES[0]; // 找不到就繞回下週一第一節

  return { current, next, dow, nowMin };
}

// ---------- 畫面渲染 ----------
const heroTag = document.getElementById("heroTag");
const heroSubject = document.getElementById("heroSubject");
const heroTeacher = document.getElementById("heroTeacher");
const heroTime = document.getElementById("heroTime");
const heroCountdown = document.getElementById("heroCountdown");
const progressFill = document.getElementById("progressFill");
const progressTrack = document.getElementById("progressTrack");

const nextSubject = document.getElementById("nextSubject");
const nextTeacher = document.getElementById("nextTeacher");
const nextTime = document.getElementById("nextTime");

function weekdayText(wd) {
  return "週" + WEEKDAY_LABELS[wd];
}

function renderHero(status, now) {
  const { current, next } = status;

  if (current) {
    heroTag.textContent = "現在 · " + current.period.label;
    heroSubject.textContent = current.subject;
    heroTeacher.textContent = current.teacher || "";
    heroTime.textContent = `${current.period.start}–${current.period.end}`;

    const startMin = timeToMinutes(current.period.start);
    const endMin = timeToMinutes(current.period.end);
    const nowMin = now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60;
    const pct = Math.min(100, Math.max(0, ((nowMin - startMin) / (endMin - startMin)) * 100));
    progressTrack.style.display = "block";
    progressFill.style.width = pct + "%";

    const remainSec = Math.max(0, Math.round((endMin - nowMin) * 60));
    heroCountdown.textContent = `距離下課還有 ${formatCountdown(remainSec)}`;
  } else {
    progressTrack.style.display = "none";
    progressFill.style.width = "0%";
    const dow = status.dow;
    if (dow === 0 || dow === 6) {
      heroTag.textContent = "假日";
      heroSubject.textContent = "沒有課";
      heroTeacher.textContent = "";
      heroTime.textContent = "";
      heroCountdown.textContent = next
        ? `下次上課：${weekdayText(next.wd)} ${next.period.label} ${next.subject}`
        : "";
    } else {
      heroTag.textContent = "下課 / 空堂";
      heroSubject.textContent = next ? `等待 ${next.subject}` : "今天課程已結束";
      heroTeacher.textContent = "";
      heroTime.textContent = "";
      if (next) {
        const nowMin = now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60;
        const startMin = next.wd === status.dow ? timeToMinutes(next.period.start) : null;
        const remainSec = startMin !== null ? Math.max(0, Math.round((startMin - nowMin) * 60)) : null;
        heroCountdown.textContent =
          remainSec !== null
            ? `距離上課還有 ${formatCountdown(remainSec)}`
            : `下次上課：${weekdayText(next.wd)} ${next.period.label}`;
      } else {
        heroCountdown.textContent = "";
      }
    }
  }
}

function renderNextStrip(status) {
  const { current, next } = status;
  if (!next) {
    nextSubject.textContent = "本週課程已結束";
    nextTeacher.textContent = "";
    nextTime.textContent = "";
    return;
  }
  // 若目前正在上課，「下一節」要排除目前這節之後最近一節；上面 getStatus 已經是找 current 之後的下一筆
  nextSubject.textContent = next.subject;
  nextTeacher.textContent = next.teacher || "";
  nextTime.textContent = (next.wd !== status.dow ? weekdayText(next.wd) + " " : "") + next.period.start;
}

// ---------- 課表格 ----------
function renderTable(status) {
  const head = document.getElementById("tableHead");
  const body = document.getElementById("tableBody");
  head.innerHTML = "";
  body.innerHTML = "";

  const corner = document.createElement("th");
  corner.textContent = "節次";
  head.appendChild(corner);

  for (let wd = 1; wd <= 5; wd++) {
    const th = document.createElement("th");
    th.textContent = weekdayText(wd);
    if (wd === status.dow) th.classList.add("today-col");
    head.appendChild(th);
  }

  PERIODS.forEach((period) => {
    const tr = document.createElement("tr");
    if (status.current && status.current.period.id === period.id) {
      tr.classList.add("current-row");
    }

    const pc = document.createElement("td");
    pc.className = "period-col";
    pc.innerHTML = `<span class="p-label">${period.label}</span><span class="p-time">${period.start}-${period.end}</span>`;
    tr.appendChild(pc);

    for (let wd = 1; wd <= 5; wd++) {
      const td = document.createElement("td");
      if (wd === status.dow) td.classList.add("today-col");
      const entry = SCHEDULE[period.id][wd];
      if (entry) {
        td.innerHTML = `<span class="cell-subject">${entry.subject}</span>${
          entry.teacher ? `<span class="cell-teacher">${entry.teacher}</span>` : ""
        }`;
      } else {
        td.innerHTML = `<span class="cell-empty">—</span>`;
      }
      tr.appendChild(td);
    }
    body.appendChild(tr);
  });
}

// ---------- 通知列（原生 App 版：由 Android 前景服務常駐更新，網頁端不需再處理） ----------
const notice = document.getElementById("notice");

// ---------- 全螢幕橫向倒數 ----------
const fsOverlay = document.getElementById("fsOverlay");
const fsTag = document.getElementById("fsTag");
const fsSubject = document.getElementById("fsSubject");
const fsCountdown = document.getElementById("fsCountdown");
const fsSub = document.getElementById("fsSub");
const fsClock = document.getElementById("fsClock");
const fullscreenBtn = document.getElementById("fullscreenBtn");
const fsExitBtn = document.getElementById("fsExitBtn");

function renderFullscreen(status, now) {
  if (!fsOverlay.classList.contains("active")) return;
  const { current, next } = status;

  fsClock.textContent =
    String(now.getHours()).padStart(2, "0") +
    ":" +
    String(now.getMinutes()).padStart(2, "0") +
    ":" +
    String(now.getSeconds()).padStart(2, "0");

  if (current) {
    fsTag.textContent = current.period.label + " 上課中";
    fsSubject.textContent = current.subject + (current.teacher ? "　" + current.teacher : "");
    const endMin = timeToMinutes(current.period.end);
    const nowMin = now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60;
    const remainSec = Math.max(0, Math.round((endMin - nowMin) * 60));
    fsCountdown.textContent = formatCountdown(remainSec);
    fsSub.textContent = next ? `下一節：${next.subject}` : "";
  } else if (status.dow >= 1 && status.dow <= 5 && next && next.wd === status.dow) {
    fsTag.textContent = "下課 / 空堂";
    fsSubject.textContent = "等待 " + next.subject;
    const startMin = timeToMinutes(next.period.start);
    const nowMin = now.getHours() * 60 + now.getMinutes() + now.getSeconds() / 60;
    const remainSec = Math.max(0, Math.round((startMin - nowMin) * 60));
    fsCountdown.textContent = formatCountdown(remainSec);
    fsSub.textContent = `${next.period.label}　${next.period.start}`;
  } else {
    fsTag.textContent = next ? "下次上課" : "";
    fsSubject.textContent = next ? next.subject : "沒有課程";
    fsCountdown.textContent = "--:--";
    fsSub.textContent = next ? `${weekdayText(next.wd)} ${next.period.label} ${next.period.start}` : "";
  }
}

async function enterFullscreenCountdown() {
  fsOverlay.classList.add("active");
  try {
    if (fsOverlay.requestFullscreen) await fsOverlay.requestFullscreen();
  } catch (e) {
    /* 全螢幕被拒絕也沒關係，覆蓋層仍會顯示 */
  }
  try {
    if (screen.orientation && screen.orientation.lock) await screen.orientation.lock("landscape");
  } catch (e) {
    /* 部分瀏覽器（例如 iOS Safari）不支援鎖定方向，使用者需自行轉動手機 */
  }
  tick();
}

function exitFullscreenCountdown() {
  fsOverlay.classList.remove("active");
  if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
  try {
    if (screen.orientation && screen.orientation.unlock) screen.orientation.unlock();
  } catch (e) {}
}

fullscreenBtn.addEventListener("click", enterFullscreenCountdown);
fsExitBtn.addEventListener("click", exitFullscreenCountdown);
document.addEventListener("fullscreenchange", () => {
  if (!document.fullscreenElement) exitFullscreenCountdown();
});

// ---------- 主更新迴圈 ----------
function tick() {
  const now = new Date();
  const status = getStatus(now);
  renderHero(status, now);
  renderNextStrip(status);
  renderTable(status);
  renderFullscreen(status, now);
}

function init() {
  document.getElementById("tableHead");
  tick();
  setInterval(tick, 1000);

  notice.textContent =
    "通知列會由 App 常駐顯示目前/下一節課程與倒數時間，每 30 秒自動更新，不會被滑掉；只有強制關閉 App 或重開機時會短暫中斷（重開機後會自動恢復）。";

  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("sw.js");
  }
}

init();
