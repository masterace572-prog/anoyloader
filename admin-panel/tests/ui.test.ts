import { test } from "node:test";
import assert from "node:assert/strict";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { readFileSync } from "node:fs";
import { Button, Segmented, StatusText, Toggle } from "../components/ui";

test("loading buttons prevent repeat submissions and announce busy state", () => {
  const html = renderToStaticMarkup(
    createElement(Button, { loading: true }, "Saving"),
  );
  assert.match(html, /aria-busy="true"/);
  assert.match(html, /disabled=""/);
  assert.match(html, /Saving/);
});
test("filter and toggle states are exposed to assistive technology", () => {
  const filters = renderToStaticMarkup(
    createElement(Segmented, {
      value: "ALL",
      onChange: () => {},
      options: [
        { value: "ALL", label: "All" },
        { value: "ACTIVE", label: "Active" },
      ],
    }),
  );
  assert.match(filters, /aria-pressed="true"/);
  assert.match(filters, /aria-pressed="false"/);
  const toggle = renderToStaticMarkup(
    createElement(Toggle, {
      checked: true,
      onChange: () => {},
      label: "Pause the app",
    }),
  );
  assert.match(toggle, /role="switch"/);
  assert.match(toggle, /aria-checked="true"/);
});
test("license status includes readable text, not color alone", () => {
  for (const status of ["ACTIVE", "BANNED", "UNUSED", "EXPIRED"]) {
    assert.match(
      renderToStaticMarkup(createElement(StatusText, { status })),
      new RegExp(status[0] + status.slice(1).toLowerCase()),
    );
  }
});
test("theme typography remains font variables and reduced motion is respected", () => {
  const config = readFileSync(
    new URL("../tailwind.config.ts", import.meta.url),
    "utf8",
  );
  assert.match(config, /var\(--font-sans\)/);
  assert.doesNotMatch(config, /--font-sans-rgb/);
  const css = readFileSync(
    new URL("../app/globals.css", import.meta.url),
    "utf8",
  );
  assert.match(css, /prefers-reduced-motion/);
  assert.match(css, /data-theme/);
});
