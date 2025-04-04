import {
  type GenericInitialData,
  type InputOutputModel,
  getInitialDataService,
} from "@knime/scripting-editor";

import type { ExecutableOption } from "./types/common";

export type PythonInitialData = GenericInitialData & {
  hasPreview: boolean;
  outputObjects: InputOutputModel[];
  executableOptionsList: ExecutableOption[];
};

export const getPythonInitialDataService = () => ({
  ...getInitialDataService(),
  getInitialData: async () =>
    (await getInitialDataService().getInitialData()) as PythonInitialData,
});
