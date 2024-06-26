// eslint-disable-next-line spaced-comment
/// <reference types="vite/client vite-svg-loader" />

interface ImportMetaEnv {
  readonly MODE: string;
  readonly DEV: boolean;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
