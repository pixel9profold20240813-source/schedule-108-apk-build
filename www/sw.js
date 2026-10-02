const CACHE = "schedule-108-v5";
const ASSETS = ["./", "index.html", "style.css", "app.js", "data.js", "manifest.json", "icons/icon-192.png", "icons/icon-512.png"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(ASSETS)));
  self.skipWaiting();
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
  );
  self.clients.claim();
});

self.addEventListener("fetch", (e) => {
  e.respondWith(caches.match(e.request).then((res) => res || fetch(e.request)));
});

self.addEventListener("notificationclick", (e) => {
  e.notification.close();
  e.waitUntil(
    self.clients.matchAll({ type: "window" }).then((list) => {
      if (list.length > 0) return list[0].focus();
      return self.clients.openWindow("./");
    })
  );
});

// 這段程式碼取決於裝置/瀏覽器是否支援 Periodic Background Sync，
// 系統會自行決定實際觸發間隔（通常遠比 15 分鐘長），僅作為盡力更新。
self.addEventListener("periodicsync", (e) => {
  if (e.tag === "update-class-status") {
    e.waitUntil(updateStatusNotification());
  }
});

async function updateStatusNotification() {
  try {
    importScripts("data.js");
  } catch (err) {
    return;
  }
  const now = new Date();
  const dow = now.getDay();
  const nowMin = now.getHours() * 60 + now.getMinutes();

  function timeToMinutes(t) {
    const [h, m] = t.split(":").map(Number);
    return h * 60 + m;
  }

  const entries = [];
  for (let wd = 1; wd <= 5; wd++) {
    for (const period of PERIODS) {
      const entry = SCHEDULE[period.id][wd];
      if (entry) entries.push({ wd, period, ...entry });
    }
  }
  const current = entries.find(
    (e) => e.wd === dow && nowMin >= timeToMinutes(e.period.start) && nowMin < timeToMinutes(e.period.end)
  );
  const next = entries.find((e) => e.wd > dow || (e.wd === dow && timeToMinutes(e.period.start) > nowMin)) || entries[0];

  let title, body;
  if (current) {
    title = `${current.period.label} · ${current.subject}`;
    body = `${current.teacher ? current.teacher + " · " : ""}${current.period.start}–${current.period.end}`;
  } else if (next) {
    title = "下課中";
    body = `下一節：${next.period.label} ${next.subject}（${next.period.start}）`;
  } else {
    title = "課表";
    body = "目前沒有課程";
  }

  return self.registration.showNotification(title, {
    body,
    tag: "class-status",
    silent: true,
    icon: "icons/icon-192.png",
    badge: "icons/icon-192.png",
  });
}
