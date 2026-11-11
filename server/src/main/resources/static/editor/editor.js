"use strict";

// Editor for the deck definition. Works on a local copy of the deck and
// PUTs the whole thing to /api/deck; the server validates, persists and
// pushes a fresh profileSync to connected clients.

const $ = (id) => document.getElementById(id);
const statusEl = $("status");

let deck = null;
let profileId = null;
let pageId = null;
let selected = null; // { row, col, keyId | null }
let formAction = null; // working copy of the selected key's action

const ACTION_TYPES = [
  ["hotkey", "ホットキー"],
  ["text", "テキスト貼り付け"],
  ["launch", "アプリ起動"],
  ["open", "URL・ファイルを開く"],
  ["media", "メディアキー"],
  ["delay", "待機（ディレイ）"],
  ["multi", "マルチアクション"],
];
const MEDIA_COMMANDS = ["PLAY_PAUSE", "NEXT", "PREV", "STOP", "VOL_UP", "VOL_DOWN", "MUTE"];
const MODIFIERS = ["CTRL", "ALT", "SHIFT", "WIN"];

function setStatus(text, isError) {
  statusEl.textContent = text;
  statusEl.classList.toggle("error", !!isError);
}

async function api(path, options) {
  const res = await fetch(path, options);
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.message || body.error || detail;
    } catch { /* keep statusText */ }
    throw new Error(`${res.status}: ${detail}`);
  }
  return res.status === 204 ? null : res.json();
}

function uid(prefix) {
  return prefix + "-" + Date.now().toString(36) + Math.floor(Math.random() * 1e4).toString(36);
}

function profile() {
  return deck?.profiles.find((p) => p.id === profileId) ?? null;
}
function page() {
  return profile()?.pages.find((pg) => pg.id === pageId) ?? null;
}
function keyAt(row, col) {
  return page()?.keys.find((k) => k.row === row && k.col === col) ?? null;
}

function defaultAction(type) {
  switch (type) {
    case "hotkey": return { type: "hotkey", modifiers: ["CTRL"], key: "S" };
    case "text": return { type: "text", text: "", restoreClipboard: true };
    case "launch": return { type: "launch", path: "", args: [] };
    case "open": return { type: "open", target: "https://" };
    case "media": return { type: "media", command: "PLAY_PAUSE" };
    case "delay": return { type: "delay", millis: 500 };
    case "multi": return { type: "multi", steps: [{ type: "delay", millis: 300 }] };
    default: return { type: "hotkey", modifiers: [], key: "A" };
  }
}

/* ---------- action field builders (mutate the given action object) ---------- */

function labeled(text, input) {
  const label = document.createElement("label");
  label.append(text, input);
  return label;
}
function textInput(value, onChange, placeholder) {
  const input = document.createElement("input");
  input.type = "text";
  input.value = value ?? "";
  if (placeholder) input.placeholder = placeholder;
  input.addEventListener("input", () => onChange(input.value));
  return input;
}

function buildActionFields(container, action, allowMulti) {
  container.innerHTML = "";
  if (action.type === "hotkey") {
    const checks = document.createElement("div");
    checks.className = "checks";
    for (const mod of MODIFIERS) {
      const label = document.createElement("label");
      const box = document.createElement("input");
      box.type = "checkbox";
      box.checked = (action.modifiers ?? []).includes(mod);
      box.addEventListener("change", () => {
        const set = new Set(action.modifiers ?? []);
        box.checked ? set.add(mod) : set.delete(mod);
        action.modifiers = MODIFIERS.filter((m) => set.has(m));
      });
      label.append(box, mod);
      checks.appendChild(label);
    }
    container.append(checks, labeled("キー", textInput(action.key, (v) => { action.key = v.toUpperCase(); }, "例: S / F5 / ENTER / SPACE")));
  } else if (action.type === "text") {
    const area = document.createElement("textarea");
    area.rows = 3;
    area.value = action.text ?? "";
    area.addEventListener("input", () => { action.text = area.value; });
    const restoreLabel = document.createElement("label");
    restoreLabel.className = "inline";
    const box = document.createElement("input");
    box.type = "checkbox";
    box.checked = action.restoreClipboard !== false;
    box.addEventListener("change", () => { action.restoreClipboard = box.checked; });
    restoreLabel.append(box, "貼り付け後に元のクリップボードを復元する");
    container.append(labeled("貼り付ける文字列", area), restoreLabel);
  } else if (action.type === "launch") {
    container.append(
      labeled("実行ファイル", textInput(action.path, (v) => { action.path = v; }, "例: notepad.exe")),
      labeled("引数（スペース区切り）", textInput((action.args ?? []).join(" "), (v) => {
        action.args = v.split(/\s+/).filter(Boolean);
      }))
    );
  } else if (action.type === "open") {
    container.append(labeled("URL・ファイル・フォルダ", textInput(action.target, (v) => { action.target = v; })));
  } else if (action.type === "media") {
    const select = document.createElement("select");
    for (const cmd of MEDIA_COMMANDS) {
      const opt = document.createElement("option");
      opt.value = cmd; opt.textContent = cmd;
      select.appendChild(opt);
    }
    select.value = action.command ?? "PLAY_PAUSE";
    select.addEventListener("change", () => { action.command = select.value; });
    container.append(labeled("コマンド", select));
  } else if (action.type === "delay") {
    const input = document.createElement("input");
    input.type = "number";
    input.min = "0"; input.step = "50";
    input.value = action.millis ?? 500;
    input.addEventListener("input", () => { action.millis = Number(input.value) || 0; });
    container.append(labeled("待機時間（ミリ秒）", input));
  } else if (action.type === "multi") {
    const stepsBox = document.createElement("div");
    stepsBox.id = "stepsBox";
    const addBtn = document.createElement("button");
    addBtn.type = "button";
    addBtn.textContent = "＋ステップ追加";
    addBtn.addEventListener("click", () => {
      action.steps.push(defaultAction("hotkey"));
      renderSteps(stepsBox, action);
    });
    container.append(stepsBox, addBtn);
    renderSteps(stepsBox, action);
  }
}

function renderSteps(stepsBox, multiAction) {
  stepsBox.innerHTML = "";
  multiAction.steps.forEach((step, index) => {
    const box = document.createElement("div");
    box.className = "step";
    const head = document.createElement("div");
    head.className = "step-head";
    const select = document.createElement("select");
    for (const [value, label] of ACTION_TYPES) {
      if (value === "multi") continue;
      const opt = document.createElement("option");
      opt.value = value; opt.textContent = label;
      select.appendChild(opt);
    }
    select.value = step.type;
    select.addEventListener("change", () => {
      multiAction.steps[index] = defaultAction(select.value);
      renderSteps(stepsBox, multiAction);
    });
    const mkBtn = (text, fn) => {
      const b = document.createElement("button");
      b.type = "button"; b.textContent = text;
      b.addEventListener("click", fn);
      return b;
    };
    head.append(select,
      mkBtn("↑", () => { if (index > 0) { [multiAction.steps[index - 1], multiAction.steps[index]] = [multiAction.steps[index], multiAction.steps[index - 1]]; renderSteps(stepsBox, multiAction); } }),
      mkBtn("↓", () => { if (index < multiAction.steps.length - 1) { [multiAction.steps[index + 1], multiAction.steps[index]] = [multiAction.steps[index], multiAction.steps[index + 1]]; renderSteps(stepsBox, multiAction); } }),
      mkBtn("✕", () => { multiAction.steps.splice(index, 1); renderSteps(stepsBox, multiAction); })
    );
    const fields = document.createElement("div");
    buildActionFields(fields, step, false);
    box.append(head, fields);
    stepsBox.appendChild(box);
  });
}

/* ---------- rendering ---------- */

function renderProfiles() {
  const select = $("profileSelect");
  select.innerHTML = "";
  for (const p of deck.profiles) {
    const opt = document.createElement("option");
    opt.value = p.id; opt.textContent = p.name;
    select.appendChild(opt);
  }
  select.value = profileId;
}

function renderPages() {
  const tabs = $("pageTabs");
  tabs.innerHTML = "";
  for (const pg of profile()?.pages ?? []) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.textContent = pg.name;
    btn.classList.toggle("current", pg.id === pageId);
    btn.addEventListener("click", () => { pageId = pg.id; selected = null; renderAll(); });
    tabs.appendChild(btn);
  }
  const pg = page();
  $("pageName").value = pg?.name ?? "";
  $("pageRows").value = pg?.rows ?? 3;
  $("pageCols").value = pg?.cols ?? 5;
}

function iconContent(icon) {
  if (icon?.type === "image") {
    const img = document.createElement("img");
    img.src = "/" + icon.value;
    img.alt = "";
    return img;
  }
  const span = document.createElement("span");
  span.textContent = icon?.value ?? "";
  return span;
}

function renderGrid() {
  const grid = $("grid");
  grid.innerHTML = "";
  const pg = page();
  if (!pg) return;
  grid.style.gridTemplateColumns = `repeat(${pg.cols}, 96px)`;
  for (let row = 0; row < pg.rows; row++) {
    for (let col = 0; col < pg.cols; col++) {
      const key = keyAt(row, col);
      const cell = document.createElement("button");
      cell.type = "button";
      cell.className = "cell" + (key ? "" : " empty");
      if (selected && selected.row === row && selected.col === col) cell.classList.add("selected");
      if (key) {
        const icon = document.createElement("span");
        icon.className = "icon";
        icon.appendChild(iconContent(key.icon));
        const title = document.createElement("span");
        title.textContent = key.title || "";
        cell.append(icon, title);
      } else {
        cell.textContent = "＋";
      }
      cell.addEventListener("click", () => selectCell(row, col));
      grid.appendChild(cell);
    }
  }
}

function selectCell(row, col) {
  const key = keyAt(row, col);
  selected = { row, col, keyId: key?.id ?? null };
  formAction = key ? JSON.parse(JSON.stringify(key.action)) : defaultAction("hotkey");
  renderGrid();
  renderForm(key);
}

function renderForm(key) {
  $("formTitle").textContent = key
    ? `キー編集（${selected.row + 1}行 ${selected.col + 1}列）`
    : selected ? `新規キー（${selected.row + 1}行 ${selected.col + 1}列）` : "キーを選択してください";
  $("keyTitle").value = key?.title ?? "";
  $("iconType").value = key?.icon?.type ?? "emoji";
  $("iconValue").value = key?.icon?.type === "image" ? "" : (key?.icon?.value ?? "");
  $("iconFile").value = "";
  toggleIconRows();
  $("actionType").value = formAction?.type ?? "hotkey";
  buildActionFields($("actionFields"), formAction ?? defaultAction("hotkey"), true);
}

function toggleIconRows() {
  const isImage = $("iconType").value === "image";
  $("iconValueRow").hidden = isImage;
  $("iconFileRow").hidden = !isImage;
}

function renderAll() {
  renderProfiles();
  renderPages();
  renderGrid();
  if (selected) {
    renderForm(keyAt(selected.row, selected.col));
  } else {
    $("formTitle").textContent = "キーを選択してください";
  }
}

/* ---------- operations ---------- */

async function resolveIcon() {
  const type = $("iconType").value;
  if (type !== "image") return { type, value: $("iconValue").value };
  const file = $("iconFile").files[0];
  if (!file) {
    const key = selected?.keyId ? page().keys.find((k) => k.id === selected.keyId) : null;
    if (key?.icon?.type === "image") return key.icon; // keep existing image
    throw new Error("画像ファイルを選んでください");
  }
  const form = new FormData();
  form.append("file", file);
  const result = await api("/api/icons", { method: "POST", body: form });
  return { type: "image", value: result.path };
}

async function applyKey() {
  if (!selected) { setStatus("まずグリッドのセルを選んでください", true); return; }
  const pg = page();
  const icon = await resolveIcon();
  const existing = selected.keyId ? pg.keys.find((k) => k.id === selected.keyId) : null;
  const keyDef = {
    id: existing?.id ?? uid("key"),
    row: selected.row,
    col: selected.col,
    title: $("keyTitle").value,
    icon,
    action: JSON.parse(JSON.stringify(formAction)),
  };
  if (existing) {
    pg.keys[pg.keys.indexOf(existing)] = keyDef;
  } else {
    pg.keys.push(keyDef);
  }
  selected.keyId = keyDef.id;
  renderGrid();
  setStatus("下書きに反映しました。右上の「保存」で確定します。");
}

async function saveAll() {
  try {
    const saved = await api("/api/deck", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(deck),
    });
    deck = saved;
    setStatus("保存しました。接続中のクライアントへ再配信済みです。");
    renderAll();
  } catch (e) {
    setStatus("保存に失敗: " + e.message, true);
  }
}

/* ---------- wiring ---------- */

$("profileSelect").addEventListener("change", (e) => {
  profileId = e.target.value;
  pageId = profile()?.pages[0]?.id ?? null;
  selected = null;
  renderAll();
});
$("addProfileBtn").addEventListener("click", () => {
  const id = uid("prof");
  deck.profiles.push({ id, name: "新規プロファイル", appPattern: null,
    pages: [{ id: uid("page"), name: "メイン", rows: 3, cols: 5, keys: [] }] });
  profileId = id; pageId = deck.profiles.at(-1).pages[0].id; selected = null;
  renderAll();
});
$("renameProfileBtn").addEventListener("click", () => {
  const name = window.prompt("プロファイル名", profile()?.name ?? "");
  if (name) { profile().name = name; renderAll(); }
});
$("deleteProfileBtn").addEventListener("click", () => {
  if (deck.profiles.length <= 1) { setStatus("プロファイルは最低1つ必要です", true); return; }
  if (!window.confirm("このプロファイルを削除しますか？")) return;
  deck.profiles = deck.profiles.filter((p) => p.id !== profileId);
  profileId = deck.profiles[0].id; pageId = deck.profiles[0].pages[0]?.id ?? null; selected = null;
  renderAll();
});
$("addPageBtn").addEventListener("click", () => {
  const pg = { id: uid("page"), name: "新規ページ", rows: 3, cols: 5, keys: [] };
  profile().pages.push(pg);
  pageId = pg.id; selected = null;
  renderAll();
});
$("deletePageBtn").addEventListener("click", () => {
  const prof = profile();
  if (prof.pages.length <= 1) { setStatus("ページは最低1つ必要です", true); return; }
  if (!window.confirm("このページを削除しますか？")) return;
  prof.pages = prof.pages.filter((pg) => pg.id !== pageId);
  pageId = prof.pages[0].id; selected = null;
  renderAll();
});
$("pageName").addEventListener("input", (e) => { if (page()) { page().name = e.target.value; renderPages(); } });
$("pageRows").addEventListener("change", (e) => changeGridSize("rows", Number(e.target.value)));
$("pageCols").addEventListener("change", (e) => changeGridSize("cols", Number(e.target.value)));
function changeGridSize(which, value) {
  const pg = page();
  if (!pg || !(value >= 1 && value <= 8)) return;
  pg[which] = value;
  const before = pg.keys.length;
  pg.keys = pg.keys.filter((k) => k.row < pg.rows && k.col < pg.cols);
  if (pg.keys.length < before) setStatus("グリッド外になったキーを下書きから除きました", true);
  selected = null;
  renderPages(); renderGrid();
}

$("iconType").addEventListener("change", toggleIconRows);
$("actionType").addEventListener("change", (e) => {
  formAction = defaultAction(e.target.value);
  buildActionFields($("actionFields"), formAction, true);
});
$("applyKeyBtn").addEventListener("click", () => { applyKey().catch((e) => setStatus(e.message, true)); });
$("deleteKeyBtn").addEventListener("click", () => {
  if (!selected?.keyId) { setStatus("削除するキーが選択されていません", true); return; }
  const pg = page();
  pg.keys = pg.keys.filter((k) => k.id !== selected.keyId);
  selected = null;
  renderAll();
  setStatus("下書きから削除しました。「保存」で確定します。");
});
$("saveBtn").addEventListener("click", saveAll);
$("exportBtn").addEventListener("click", () => { location.href = "/api/export"; });
$("importFile").addEventListener("change", async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  try {
    const parsed = JSON.parse(await file.text());
    deck = await api("/api/import", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(parsed),
    });
    profileId = deck.profiles[0]?.id ?? null;
    pageId = deck.profiles[0]?.pages[0]?.id ?? null;
    selected = null;
    renderAll();
    setStatus("インポートしました（旧設定は deck.json.bak に残っています）。");
  } catch (err) {
    setStatus("インポートに失敗: " + err.message, true);
  }
  e.target.value = "";
});

(async () => {
  try {
    deck = await api("/api/deck");
    profileId = deck.profiles[0]?.id ?? null;
    pageId = deck.profiles[0]?.pages[0]?.id ?? null;
    renderAll();
    setStatus("読み込みました。編集後は「保存」を押してください。");
  } catch (e) {
    setStatus("読み込みに失敗: " + e.message, true);
  }
})();
