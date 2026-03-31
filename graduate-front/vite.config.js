import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";
import { visualizer } from "rollup-plugin-visualizer";
import path from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = path.dirname(fileURLToPath(import.meta.url));

// This repo still uses the classic uni-app root layout (main.js/pages.json/App.vue in project root).
// The standalone CLI defaults UNI_INPUT_DIR to "<root>/src", which makes the H5 main.js transform miss.
process.env.UNI_INPUT_DIR = projectRoot;

// https://vitejs.dev/config/
const useUniH5Vue =
  !process.env.UNI_PLATFORM ||
  process.env.UNI_PLATFORM === "h5" ||
  process.env.UNI_PLATFORM === "web";

export default defineConfig({
  resolve: {
    alias: useUniH5Vue
      ? [
          { find: /^vue$/, replacement: "@dcloudio/uni-h5-vue" },
          {
            find: /^vue\/package\.json$/,
            replacement: "@dcloudio/uni-h5-vue/package.json",
          },
        ]
      : [],
  },
  optimizeDeps: {
    // `@dcloudio/uni-app` expects the DCloud-patched Vue runtime on H5.
    // Excluding it avoids Vite pre-bundling against the plain `vue` package.
    exclude: useUniH5Vue ? ["@dcloudio/uni-app", "vue"] : [],
  },
  plugins: [
    uni(),
	visualizer()
  ],
  css: {  
	preprocessorOptions: {  
	  scss: {  
		// 取消sass废弃API的报警
		silenceDeprecations: ['legacy-js-api', 'color-functions', 'import'],  
	  },  
	},  
  },
  server: {
    port: 5100,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:5050",
        changeOrigin: true,
      },
    },
    fs: {
        // Allow serving files from one level up to the project root
        allow: ['..']
    }
},
});
