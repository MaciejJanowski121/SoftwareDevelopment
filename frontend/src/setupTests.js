
import '@testing-library/jest-dom';


const originalWarn = console.warn;

beforeAll(() => {
    console.warn = (msg, ...args) => {
        if (typeof msg === "string" && msg.includes("React Router Future Flag Warning")) return;
        originalWarn(msg, ...args);
    };
});