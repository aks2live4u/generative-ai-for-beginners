import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import { VitePWA } from "vite-plugin-pwa";

// The Android WebView shell loads this build from file:///android_asset/www/,
// where absolute paths like "/assets/foo.js" resolve to the device's
// filesystem root instead of the bundle -- so that build needs relative
// paths ("./assets/foo.js") and has no use for a service worker (there's
// nothing to cache offline when the whole app is already bundled locally).
// Run with `npm run build:android` (sets BUILD_TARGET=android).
const isAndroidBuild = process.env.BUILD_TARGET === "android";

export default defineConfig({
  base: isAndroidBuild ? "./" : "/",
  plugins: [
    react(),
    ...(isAndroidBuild
      ? []
      : [
          VitePWA({
            registerType: "autoUpdate",
            includeAssets: ["favicon.svg"],
            manifest: {
              name: "CareerPilot AI",
              short_name: "CareerPilot",
              description: "Your autonomous AI career agent",
              theme_color: "#0f172a",
              background_color: "#0f172a",
              display: "standalone",
              orientation: "portrait",
              start_url: "/",
              icons: [
                { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
                { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
                { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
              ],
            },
          }),
        ]),
  ],
  server: {
    host: true,
    port: 5173,
  },
  build: {
    outDir: isAndroidBuild ? "dist-android" : "dist",
  },
});
