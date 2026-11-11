"use strict";

// Sprint 1 minimal client: connect, authenticate, render the grid from
// profileSync, send keyEvent on tap, flash the key on actionResult.
// Reconnect with exponential backoff. Pairing UI arrives in Sprint 5;
// for now the token comes from ?token= or localStorage.

const statusEl = document.getElementById("status");
const pagesEl = document.getElementById("pages");
const gridEl = document.getElementById("grid");
const toastEl = document.getElementById("toast");

let ws = null;
let deck = null;
let currentProfileId = null;
let currentPageId = null;
let backoffMs = 1000;
let toastTimer = null;

// A pairing code (?pair=) is used once for the first auth; the server
// exchanges it for a session token, which is then kept in localStorage.
const pairCode = new URLSearchParams(location.search).get("pair");
if (pairCode) {
  history.replaceState(null, "", location.pathname);
}
let pairPending = !!pairCode;

function token() {
  if (pairPending) return pairCode;
  const fromUrl = new URLSearchParams(location.search).get("token");
  if (fromUrl) {
    localStorage.setItem("deckToken", fromUrl);
    return fromUrl;
  }
  let stored = localStorage.getItem("deckToken");
  if (!stored) {
    stored = window.prompt("接続トークンを入力してください") || "";
    if (stored) localStorage.setItem("deckToken", stored);
  }
  return stored;
}

function setStatus(text, connected) {
  statusEl.textContent = text;
  statusEl.classList.toggle("connected", !!connected);
}

function toast(text) {
  toastEl.textContent = text;
  toastEl.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toastEl.classList.remove("show"), 1600);
}

function connect() {
  const port = location.port || "8081";
  const url = `ws://${location.hostname}:${port}/ws`;
  setStatus("接続中…", false);
  ws = new WebSocket(url);

  ws.addEventListener("open", () => {
    ws.send(JSON.stringify({ type: "auth", token: token() }));
  });

  ws.addEventListener("message", (event) => {
    let msg;
    try { msg = JSON.parse(event.data); } catch { return; }
    if (msg.type === "authOk") {
      backoffMs = 1000;
      pairPending = false;
      if (msg.sessionToken) {
        localStorage.setItem("deckToken", msg.sessionToken);
        toast("ペアリング完了。次回から自動接続します");
      }
      setStatus("接続済み", true);
    } else if (msg.type === "authFailed") {
      setStatus("認証失敗", false);
      pairPending = false;
      localStorage.removeItem("deckToken");
      toast("トークンが違います。PCの /pair から再ペアリングしてください");
    } else if (msg.type === "profileSync") {
      deck = msg.deck;
      if (!currentProfileId && deck.profiles.length > 0) {
        currentProfileId = deck.profiles[0].id;
        currentPageId = deck.profiles[0].pages[0]?.id ?? null;
      }
      render();
    } else if (msg.type === "actionResult") {
      flashKey(msg.keyId, msg.success);
      if (!msg.success) toast(msg.message || "実行に失敗しました");
    }
  });

  ws.addEventListener("close", () => {
    setStatus("切断（再接続待ち）", false);
    setTimeout(connect, backoffMs);
    backoffMs = Math.min(backoffMs * 2, 30000);
  });

  ws.addEventListener("error", () => {
    try { ws.close(); } catch { /* already closed */ }
  });
}

function currentPage() {
  const profile = deck?.profiles.find((p) => p.id === currentProfileId);
  return profile?.pages.find((pg) => pg.id === currentPageId) ?? null;
}

function render() {
  if (!deck) return;
  const profile = deck.profiles.find((p) => p.id === currentProfileId);
  pagesEl.innerHTML = "";
  if (profile && profile.pages.length > 1) {
    for (const pg of profile.pages) {
      const btn = document.createElement("button");
      btn.textContent = pg.name;
      btn.classList.toggle("current", pg.id === currentPageId);
      btn.addEventListener("click", () => { currentPageId = pg.id; render(); });
      pagesEl.appendChild(btn);
    }
  }
  const page = currentPage();
  gridEl.innerHTML = "";
  if (!page) return;
  gridEl.style.gridTemplateColumns = `repeat(${page.cols}, 1fr)`;
  const sorted = [...page.keys].sort((a, b) => a.row - b.row || a.col - b.col);
  for (const key of sorted) {
    const btn = document.createElement("button");
    btn.className = "key";
    btn.dataset.keyId = key.id;
    btn.style.gridRow = String(key.row + 1);
    btn.style.gridColumn = String(key.col + 1);
    const icon = document.createElement("span");
    icon.className = "icon";
    if (key.icon && key.icon.type === "image") {
      const img = document.createElement("img");
      img.src = "/" + key.icon.value;
      img.alt = "";
      img.style.width = "62%";
      img.style.height = "62%";
      img.style.objectFit = "contain";
      icon.appendChild(img);
    } else {
      icon.textContent = key.icon ? key.icon.value : "";
    }
    const title = document.createElement("span");
    title.textContent = key.title || "";
    btn.append(icon, title);
    btn.addEventListener("click", () => sendKeyEvent(key.id));
    gridEl.appendChild(btn);
  }
}

function sendKeyEvent(keyId) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    toast("未接続です");
    return;
  }
  ws.send(JSON.stringify({
    type: "keyEvent",
    profileId: currentProfileId,
    pageId: currentPageId,
    keyId,
    phase: "down",
  }));
}

function flashKey(keyId, success) {
  const el = gridEl.querySelector(`[data-key-id="${CSS.escape(keyId)}"]`);
  if (!el) return;
  const cls = success ? "flash-ok" : "flash-error";
  el.classList.add(cls);
  setTimeout(() => el.classList.remove(cls), 350);
}

// PWA: cache the app shell so the deck opens instantly.
if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("/deck/sw.js").catch(() => {});
}

// Keep the screen awake while the deck is in use (where supported).
if ("wakeLock" in navigator) {
  const requestWakeLock = () => navigator.wakeLock.request("screen").catch(() => {});
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") requestWakeLock();
  });
  requestWakeLock();
}

connect();
