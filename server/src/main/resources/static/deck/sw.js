"use strict";

// App-shell cache for the deck client. Everything under /deck/ is
// cache-first (the shell rarely changes; bump CACHE to invalidate).
// The WebSocket and the API are never intercepted.

const CACHE = "deck-shell-v1";
const SHELL = [
  "/deck/",
  "/deck/index.html",
  "/deck/style.css",
  "/deck/app.js",
  "/deck/manifest.webmanifest",
  "/deck/icons/icon-192.png",
  "/deck/icons/icon-512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== "GET" || !url.pathname.startsWith("/deck/")) return;
  event.respondWith(
    caches.match(event.request).then((cached) => cached ?? fetch(event.request))
  );
});
